package com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
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
	 */
	public static void main(String[] args) throws IOException {
		ClientTest kcpClient = new ClientTest(13333);//
		kcpClient.NoDelay(1, 10, 2, 1);
		kcpClient.WndSize(1000, 1000);
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
		Arrays.fill(buffer16, 0, 8192 * 2 - 2, (byte) 'F');
		Arrays.fill(buffer16, 8192 * 2 - 1, 8192 * 2, (byte) '!');
		int i = 2;
		while (i > 0) {
//			String content = "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK"
//					+ "KKKKKKKKKK" + "KKKKKKKKKK" + "" + "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK" + "KKKKKKKKKK"
//					+ "KKKKKKKKKK" + "KKKKKKKKKK" + "" + i;
			// byte[] buffer = content.getBytes(Charset.forName("utf-8"));
			// kcpClient.send(buffer8);
			// System.out.println(i);
			i--;
		}
		while (true) {
			BufferedReader bufr = new BufferedReader(new InputStreamReader(System.in));
			String line = bufr.readLine();
			line.getBytes(Charset.forName("utf-8"));
			kcpClient.send(line.getBytes(Charset.forName("utf-8")));
		}

	}
}
