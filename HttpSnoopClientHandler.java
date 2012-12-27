package Snoop;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

public class HttpSnoopClientHandler extends SimpleChannelUpstreamHandler{
	private boolean readingChunks;
	private volatile Channel outChat;
	
	public HttpSnoopClientHandler(Channel outChat){
		this.outChat = outChat;
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)throws Exception{
		if(!readingChunks){
			HttpResponse response = (HttpResponse)e.getMessage();
			
			System.out.println("STATUS: " + response.getStatus());
			System.out.println("VERSION: " + response.getProtocolVersion());
			System.out.println();
			
			if(!response.getHeaderNames().isEmpty()){
				for(String name: response.getHeaderNames()){
					for(String value : response.getHeaders(name)){
						System.out.println("HEADER: "+name+" = "+value);
					}
				}
				System.out.println();
			}
			
			if(response.isChunked()){
				readingChunks = true;
				System.out.println("CHUKED CONTENT {");
			}else {
				ChannelBuffer content = response.getContent();
				if(content.readable()){
					 System.out.println("CONTENT {");
					 System.out.println(content.toString(CharsetUtil.UTF_8));
					 System.out.println("} END OF CONTENT");
				}
			}
			outChat.write(response).addListener(ChannelFutureListener.CLOSE);
		}else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if(chunk.isLast()){
				readingChunks = false;
				System.out.println("} END OF CHUNKED CONTENT");
				outChat.write(chunk).addListener(ChannelFutureListener.CLOSE);
			}else{
				outChat.write(chunk).addListener(ChannelFutureListener.CLOSE);
				System.out.print(chunk.getContent().toString(CharsetUtil.UTF_8));
				System.out.flush();
			}
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
		e.getCause().printStackTrace();
		closeOnFlush(e.getChannel());
	}
	
	static void closeOnFlush(Channel ch){
		if(ch.isConnected()){
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
