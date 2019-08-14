package com;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class KcpClient extends KCP implements Runnable {

	private static DatagramSocket datagramSocket;
	private static DatagramPacket datagramPacket;
	public Queue<byte[]> rcv_byte_que;
	public Queue<byte[]> send_byte_que;

	private InetSocketAddress remote;
	private volatile boolean running = true;
	private final static Object waitLock = new Object();
	public final KCP kcp;
	private static byte[] receMsgs = new byte[2048];
	private volatile boolean needUpdate;
	private long lastTime;
	private int timeout;

	public KcpClient(long conv_) throws SocketException, UnknownHostException {
		super(conv_);
		kcp = this;
		rcv_byte_que = new LinkedBlockingDeque<>();
		send_byte_que = new LinkedBlockingQueue<>();
		datagramSocket = new DatagramSocket();
	}

	// udp发送数据
	@Override
	protected void output(byte[] buffer, int size) {
		try {
			datagramPacket = new DatagramPacket(buffer, size);
			datagramPacket.setAddress(InetAddress.getByName("127.0.0.1"));// 设置信息发送的ip地址,255.255.255.255受限的局域网
			datagramPacket.setPort(33333);// 设置信息发送到的端口
			datagramSocket.send(datagramPacket);
			// System.out.println(remote.getPort());
			// datagramSocket.close();//发送完关闭即可，接下来就是监听30000端口
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// UDP收到消息
	public void UDP_Input() throws SocketException {
		// 创建一个数据报套接字，并将其绑定到指定port上
		// DatagramPacket(byte buf[], int length),建立一个字节数组来接收UDP包
		datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
		try {
			datagramSocket.receive(datagramPacket);
			// System.out.println("客户端接收到数据回送");
			byte[] bytes = Arrays.copyOf(receMsgs, datagramPacket.getLength());
			this.rcv_byte_que.add(bytes);// 放入缓冲队列
			this.needUpdate = true;
			this.lastTime = System.currentTimeMillis(); // 接收数据（ACK或者真正的数据或者其他数据）的时候，获得系统时间作为最后时间，用于超时检查时间
			// System.out.println("数据接受队列大小" + rcv_byte_que.size());
		} catch (Exception e) {
			// System.out.println("超时未获得数据");
			e.printStackTrace();
		}
	}

	public void connect(InetSocketAddress addr) {
		this.remote = addr;
	}

	/**
	 * 开启线程处理kcp状态
	 */
	public void start() {
		this.running = true;
		// 启动数据接受线程
		ReceiveThread receiveThread = new ReceiveThread();
		receiveThread.start();

		// 启动这个线程
		Thread t = new Thread(this);

		t.setName("kcp client thread start");
		t.start();

	}

	/**
	 * UDP接收数据处理线程
	 * 
	 * @author 19026404
	 *
	 */
	public class ReceiveThread extends Thread {
		@Override
		public void run() {
			while (running) {
				try {
					UDP_Input();
//					synchronized (waitLock) {
//						waitLock.notify();
//					}
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void noDelay(int nodelay, int interval, int resend, int nc) {
		this.NoDelay(nodelay, interval, resend, nc);
	}

	/**
	 * set maximum window size: sndwnd=32, rcvwnd=32 by default
	 *
	 * @param sndwnd
	 * @aram rcvwnd
	 */
	public void wndSize(int sndwnd, int rcvwnd) {
		this.kcp.WndSize(sndwnd, rcvwnd);
	}

	/**
	 * change MTU size, default is 1400
	 *
	 * @param mtu
	 */
	public void setMtu(int mtu) {
		this.SetMtu(mtu);
	}

	/**
	 * 开启线程处理kcp状态
	 */
	@Override
	public void run() {
		long start, end;

		while (running) {
			try {
				start = System.currentTimeMillis();// 开始时间
				// 1.input
				// System.out.println("外");
				while (!this.rcv_byte_que.isEmpty()) {
					// System.out.println("内");
					// System.out.println("队列大小：" + rcv_byte_que.size());
					byte[] buffer = this.rcv_byte_que.remove();
					int result = kcp.Input(buffer);
					// System.out.println(result);
					// 返回0表示正常
					if (result != 0) {
						running = false;
						return;
					}
				}
				// 2.reveice
				int len;
				while ((len = kcp.PeekSize()) > 0) {
					// 新建存放包的byte数组
					byte[] receiveByte = new byte[len];
					// 数据长度
					int dataLength = this.kcp.Recv(receiveByte);
					if (dataLength > 0) {
						// 输出数据
						System.out.println(new String(receiveByte, 0, dataLength));
					}
				}
				// 3 Send
				while (!this.send_byte_que.isEmpty()) {
					byte[] sendBuffer = this.send_byte_que.remove();
					int sendResult = this.kcp.Send(sendBuffer);
					if (sendResult != 0) {
						// System.out.println("数据加入发送队列成功");
						running = false;
						return;
					}
				}
				// 4 update
				// 发生了ikcp_input/_send调用时，needUpdate设置true
				if (this.needUpdate) {
					// 立即刷新kcp，发送消息
					kcp.flush();
					this.needUpdate = false;
				}
				long cur = System.currentTimeMillis();
				// 当前时间>=之前设定的下次更新时间
				if (cur >= kcp.getNextUpdate()) {
					// 更新缓冲等
					kcp.Update(cur);
					kcp.setNextUpdate(kcp.Check(cur));
				}
				// start = System.currentTimeMillis();// 开始时间
				// kcp.Update(start); //
				end = System.currentTimeMillis();// 结束时间
				if (end - start < interval) {
					synchronized (waitLock) {
						try {
							// System.out.println("等待开始");
							waitLock.wait(this.interval - end + start);
							// System.out.println("等待结束");
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
				}
				if (this.timeout > 0 && lastTime > 0 && (System.currentTimeMillis() - lastTime) > this.timeout) {
					running = false;
				}
			} catch (Exception e) {
				// running = false;
				e.printStackTrace();
			}
		}
		datagramSocket.close();
		System.out.println("数据传输完毕，客户端关闭！");
	}

	/**
	 * 发送消息加入发送缓冲的队列
	 * 
	 * @param bb
	 */
	public void send(byte[] bb) {
		if (running) {
			this.send_byte_que.add(bb);
			this.needUpdate = true;
		}
	}

	/**
	 * 关掉
	 *
	 */
	public void close() {
		if (this.running) {
			this.running = false;
		}
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

//	/**
//	 * 收到客户端消息
//	 *
//	 * @param dp
//	 */
//	public static void onReceive(byte[] buffer) {
//		int result = 0;
//		if (kcp != null) {
//			result = kcp.Input(buffer);
//			// System.out.println("服务器input返回结果" + result);
//			// 返回0表示正常
//			if (result == 0) {
//				// 获取最新消息包的长度
//				int size = kcp.PeekSize();
//				if (size > 0) {
//					// 新建存放包的byte数组
//					byte[] receiveByte = new byte[size];
//					// 数据长度
//					int dataLength = kcp.Recv(receiveByte);
//					if (dataLength > 0) {
//						// 输出数据
//						System.out.println(new String(receiveByte, 0, dataLength));
//					}
//				}
//			}
//		}
//	}
}
