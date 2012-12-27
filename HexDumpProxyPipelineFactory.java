package ProxyServer;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpServerCodec;

public class HexDumpProxyPipelineFactory implements ChannelPipelineFactory{

	private final ClientSocketChannelFactory cf;
	private final String remoteHost;
	private final int remotePort;
	
	public HexDumpProxyPipelineFactory(ClientSocketChannelFactory cf, String remoteHost, int remotePort){
		this.cf = cf;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipe = Channels.pipeline();
		pipe.addLast("codec", new HttpServerCodec());
		pipe.addLast("deflater", new HttpContentCompressor());
		pipe.addLast("handler", new HexProxyInboundHandler(cf, remoteHost, remotePort));
		return pipe;
	}

}
