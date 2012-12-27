package WebSocket;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class WebSocketServer {
	private int port;
	
	public WebSocketServer(int port)
	{
		this.port=port;
	}
	
	public void run(){
		ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),Executors.newCachedThreadPool()));
		
		bootstrap.setPipelineFactory(new WebSocketPipelineFactory());
		
		bootstrap.bind(new InetSocketAddress(port));
		
	}
	
	public static void main(String arg[]){
		//int port;
		//Scanner in = new Scanner(System.in);
		//System.out.println("What is port number ?? ");
		//port=in.nextInt();
		//in.close();
		new WebSocketServer(8080).run();
	}
}
