package Snoop;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

public class HttpSnoopClient {

	private final URI uri;
	
	public HttpSnoopClient(URI uri){
		this.uri = uri;
	}
	
	public void run(){
		String scheme = uri.getScheme() == null? "http" :uri.getScheme();
		String host = uri.getHost() == null? "localhost" : uri.getHost();
		
		int port = uri.getPort();
		if(port == -1){
			if(scheme.equalsIgnoreCase("http")){
				port = 80;
			}else if (scheme.equalsIgnoreCase("https")){
				port = 443;
			}
		}
		
		if(!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")){
			System.err.println("Only HTTP(S) is supported.");
			return;
		}
		
		boolean ssl = scheme.equalsIgnoreCase("https");
		
		ClientBootstrap cbootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));
		
		//cbootstrap.setPipelineFactory(new HttpSnoopClientPipelineFactory(ssl));
		
		ChannelFuture future = cbootstrap.connect(new InetSocketAddress(host, port));
		
		Channel channel = future.awaitUninterruptibly().getChannel();
		if(!future.isSuccess()){
			future.getCause().printStackTrace();
			cbootstrap.releaseExternalResources();
			return;
		}
		
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
				HttpMethod.GET, uri.getRawPath());
		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		
		CookieEncoder httpCookieEncoder = new CookieEncoder(false);
		httpCookieEncoder.addCookie("my-cookie", "foo");
		httpCookieEncoder.addCookie("another-cookie", "bar");
		request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
		
		channel.write(request);
		channel.getCloseFuture().awaitUninterruptibly();
		cbootstrap.releaseExternalResources();
		
	}
	public static void main(String[] args) throws Exception{
		/*if(args.length != 1){
			System.err.println(
			"Usage: " + HttpSnoopClient.class.getSimpleName() +
			" <URL>");
			return;
		}*/
		URI uri = new URI("http://www.naver.com/");
		new HttpSnoopClient(uri).run();
	}

}
