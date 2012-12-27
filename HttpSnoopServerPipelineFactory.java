package Snoop;

import static org.jboss.netty.channel.Channels.*;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class HttpSnoopServerPipelineFactory implements ChannelPipelineFactory{

	ClientSocketChannelFactory cf;
	
	public HttpSnoopServerPipelineFactory(ClientSocketChannelFactory cf){
		this.cf = cf;
	}
	
	public ChannelPipeline getPipeline() throws Exception{
		ChannelPipeline pipeline = pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("deflater", new HttpContentCompressor());
		pipeline.addLast("handler", new HttpSnoopServerHandler(cf));//Handler
		return pipeline;
	}

}
