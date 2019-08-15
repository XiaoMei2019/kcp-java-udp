package com;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 
 * @author XiaoMei
 *
 */
public class ClientTest extends KcpClient {
	public ClientTest(long conv_) throws SocketException, UnknownHostException {
		super(conv_);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		ClientTest kcpClient = new ClientTest(13333);//
		kcpClient.NoDelay(1, 10, 2, 1);
		kcpClient.WndSize(2048, 2048);
		// kcpClient.setTimeout(4 * 1000);// 超时时间100000S
		kcpClient.SetMtu(1024);
		kcpClient.connect(new InetSocketAddress("127.0.0.1", 33333));
		kcpClient.start();
		// 8K
		byte[] buffer8 = new byte[8192];
		Arrays.fill(buffer8, 0, 8190, (byte) '8');
		Arrays.fill(buffer8, 8191, 8192, (byte) '!');
		// 2k
		byte[] buffer2 = new byte[2048];
		Arrays.fill(buffer2, 0, 2046, (byte) '2');
		Arrays.fill(buffer2, 2047, 2048, (byte) '!');
		// 16k
		byte[] buffer16 = new byte[8192 * 2];
		Arrays.fill(buffer16, 0, 8192 * 2 - 2, (byte) 'X');
		Arrays.fill(buffer16, 8192 * 2 - 1, 8192 * 2, (byte) '!');
		// 32K
		byte[] buffer32 = new byte[8192 * 4];
		Arrays.fill(buffer32, 0, 8192 * 4 - 2, (byte) 'F');
		Arrays.fill(buffer32, 8192 * 4 - 1, 8192 * 4, (byte) '!');
		int i = 200;
		while (i > 0) {
			kcpClient.send(buffer2);
			Thread.sleep(200);
			i--;
		}
//		while (true) {
//			BufferedReader bufr = new BufferedReader(new InputStreamReader(System.in));
//			String line = bufr.readLine();
//			line.getBytes(Charset.forName("utf-8"));
//			kcpClient.send(line.getBytes(Charset.forName("utf-8")));
//		}

	}
}
