package Snoop;

import static org.jboss.netty.handler.codec.http.HttpHeaders.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.Set;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;


public class HttpSnoopServerHandler extends SimpleChannelUpstreamHandler{

	private HttpRequest request;
	private boolean readingChunks;
	private final StringBuilder buf = new StringBuilder();
	ClientSocketChannelFactory cf;
	private volatile Channel outChat;
	
	
	public HttpSnoopServerHandler(ClientSocketChannelFactory cf){
		this.cf = cf;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx,ChannelStateEvent e)
			throws Exception{
		final Channel inboundChannel = e.getChannel();
		inboundChannel.setReadable(false);
		
		ClientBootstrap cb = new ClientBootstrap(cf);
		cb.getPipeline().addLast("handler", new HttpSnoopClientHandler(e.getChannel()));
		ChannelFuture f = cb.connect(new InetSocketAddress("211.237.1.231", 80));
		
		outChat = f.getChannel();
		f.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					inboundChannel.setReadable(true);
				}else{
					inboundChannel.close();
				}
				
			}
		});
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e )throws Exception {
		if(!readingChunks){
			HttpRequest request = this.request = (HttpRequest) e.getMessage();
			this.outChat = e.getChannel();
			
			if(is100ContinueExpected(request)){
				send100Continue(e);
			}
			
			
			//Printing Headers
			buf.setLength(0);
			buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
			buf.append("===================================\r\n");
			
			buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
			buf.append("HOSTNAME: " + getHost(request, "unknown") + "\r\n");
			buf.append("REQUEST_URI: " + request.getUri() + "\r\n\r\n");
			/*
			URI uri = new URI("http://kldp.org/");
			buf.append("URI: "+uri.getHost()+"\r\n\r\n");
			
			for(Map.Entry<String, String> h: request.getHeaders()){
				buf.append(h.getKey()+" = "+h.getValue()+"\r\n");
			}
			buf.append("\r\n");
			*/
			/*
			URI uri = new URI("http://kldp.org/");
			
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
			
			final ClientBootstrap cbootstrap = new ClientBootstrap(cf);
			
			cbootstrap.setPipelineFactory(new HttpSnoopClientPipelineFactory(ssl,e.getChannel()));
			
			final ChannelFuture future = cbootstrap.connect(new InetSocketAddress(host, port));
			
			/*
			Channel channel = future.awaitUninterruptibly().getChannel();
			if (!future.isSuccess()) {
				future.getCause().printStackTrace();
				cbootstrap.releaseExternalResources();
				return;
			}
			
			final Channel channel = future.getChannel();
			
			future.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					// TODO Auto-generated method stub
					if(!future.isSuccess()){
						future.getCause().printStackTrace();
						cbootstrap.releaseExternalResources();
						return;
					}else
						channel.close();
				}
			});
			
			
			
			//HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
			//		HttpMethod.GET, uri.getRawPath());
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
			
			*/
			
			
			
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			Map<String,List<String>> params = queryStringDecoder.getParameters();
			if(!params.isEmpty()) {
				for(Entry<String, List<String>>p : params.entrySet()){
					String key = p.getKey();
					List<String> vals = p.getValue();
					for(String val : vals){
						buf.append("PARAM: "+key+" = "+val+"\r\n");
					}
					buf.append("\r\n");
				}
			}
			
			if(request.isChunked()){
				readingChunks = true;
			}else{
				ChannelBuffer content = request.getContent();
				if(content.readable()){
					buf.append("CONTENT: "+content.toString(CharsetUtil.UTF_8)+"\r\n");
				}
				
				writeResponse(e);
			}
			
		}else{
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if(chunk.isLast()){
				readingChunks = false;
				buf.append("END OF CONTENT\r\n");
				
				HttpChunkTrailer trailer = (HttpChunkTrailer) chunk;
				if(!trailer.getHeaderNames().isEmpty()){
					buf.append("\r\n");
					for(String name: trailer.getHeaderNames()){
						for(String value: trailer.getHeaders(name)){
							buf.append("TRAILING HEADER: "+name+" = "+value+"\r\n");
						}
					}
					buf.append("\r\n");
				}
				
				writeResponse(e);
				
			}else {
				buf.append("CHUNK: "+chunk.getContent().toString(CharsetUtil.UTF_8)+"\r\n");
			}
		}
		
		
		
	}
	
	// Parameter : Http Request
	private void writeResponse(MessageEvent e){
		boolean keepAlive = isKeepAlive(request);
		
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
		response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		
		if(keepAlive){
			response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
			response.setHeader(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		
		String cookieString = request.getHeader(COOKIE);
		if(cookieString != null){
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if(!cookies.isEmpty()){
				CookieEncoder cookieEncoder = new CookieEncoder(true);
				for( Cookie cookie : cookies){
					cookieEncoder.addCookie(cookie);
					response.addHeader(SET_COOKIE, cookieEncoder.encode());
				}
			}
		}else {
			CookieEncoder cookieEncoder = new CookieEncoder(true);
			cookieEncoder.addCookie("key1","value1");
			response.addHeader(SET_COOKIE, cookieEncoder.encode());
			cookieEncoder.addCookie("key2","value2");
			response.addHeader(SET_COOKIE, cookieEncoder.encode());
		}
		
		ChannelFuture future = outChat.write(response);
		//ChannelFuture future = e.getChannel().write(response);
		
		if(!keepAlive){
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	public static void send100Continue(MessageEvent e){
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
		e.getChannel().write(response);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	throws Exception {
		e.getCause().printStackTrace();
	    e.getChannel().close();
	}
	
	static void closeOnFlush(Channel ch){
		if(ch.isConnected()){
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
