package ProxyServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class HexProxy {
	private final int localport;
	private final String remoteHost;
	private final int remoteport;
	
	public HexProxy(int localport, String remoteHost, int remoteport){
		this.localport = localport;
		this.remoteHost = remoteHost;
		this.remoteport = remoteport;
	}
	
	public void run()
	{
		ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),Executors.newCachedThreadPool()));
		
		ClientSocketChannelFactory cf = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),Executors.newCachedThreadPool());
	
		bootstrap.setPipelineFactory(new HexDumpProxyPipelineFactory(cf,remoteHost,remoteport));
		
		bootstrap.bind(new InetSocketAddress(localport));
	}
	
	public static void main(String args[])
	{
		new HexProxy(8090, "220.95.233.171", 80).run();
	}
}
