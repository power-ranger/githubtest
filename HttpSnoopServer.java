package Snoop;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpSnoopServer {

	private final int port;
	
	
	public HttpSnoopServer(int port) {
		this.port = port;
	}
	
	public void run() throws Exception{
		
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		
		ClientSocketChannelFactory cf = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		
		
		bootstrap.setPipelineFactory(new HttpSnoopServerPipelineFactory(cf)); // Setting
		
		bootstrap.bind(new InetSocketAddress(port));
		
		
		
		
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		int port = 8000;
		new HttpSnoopServer(port).run();
	}

}
