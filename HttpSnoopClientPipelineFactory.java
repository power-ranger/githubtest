package Snoop;

import static org.jboss.netty.channel.Channels.pipeline;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.ssl.SslHandler;


public class HttpSnoopClientPipelineFactory implements ChannelPipelineFactory{

	private final boolean ssl;
	private volatile Channel outChat;
	
	public HttpSnoopClientPipelineFactory(boolean ssl, Channel outChat){
		this.ssl = ssl;
		this.outChat = outChat;
	}
	
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = pipeline();
		
		if(ssl){
			SSLEngine engine = SecureChatSslContextFactory.getClientContext().createSSLEngine();
			engine.setUseClientMode(true);
			
			pipeline.addLast("ssl", new SslHandler(engine));
		}
		
		pipeline.addLast("clodec", new HttpClientCodec());
		pipeline.addLast("inflator", new HttpContentDecompressor());
		
		pipeline.addLast("handler", new HttpSnoopClientHandler(outChat));
		
		return pipeline;
	}

	

}
