#include <sys/time.h>
#include <netinet/in.h>
#include <net/ethernet.h>
#include <pcap/pcap.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <netinet/ip_icmp.h>

#define PROMISCOUS 1

struct iphdr *iph;
struct tcphdr *tcph;
struct udphdr *udph;
struct icmp *icmph;
struct pcap_t *pd;

int sockfd;

int pflag;
int rflag;
int eflag;
int cflag;
int chcnt;

char *device, *filter_rule;

void packet_analysis(unsigned char *, const struct pcap_pkthdr *, const unsigned char *);

struct printer {
	pcap_handler f;
	int type;
};

static struct printer printers[] = {
	{ packet_analysis, DLT_IEEE802 },
	{ packet_analysis, DLT_EN10MB  },
	{ NULL, 0}
};

static pcap_handler lookup_printer(int type)
{
	struct printer *p;

	for(p=printers; p->f; ++p)
		if(type == p->type)
			return p->f;

	perror("unknown data link type");
}

void packet_analysis(unsigned char *user, const struct pcap_pkthdr *h, const unsigned char *p)
{
	int j, temp;
	unsigned int length = h->len;
	struct ether_header *ep;
	unsigned short ether_type;
	unsigned char *tcpdata, *udpdata, *icmpdata;
	register unsigned int i;

	chcnt = 0;

	if(rflag) {
		while(length--) {
			printf("%02x ", *(p++));
			if( (++chcnt % 16) == 0 ) printf("\n\t");
		}
		fprintf(stdout, "\n");
		return ;
	}

	length -= sizeof(struct ether_header);

	ep = (struct ether_header *)p;
	p += sizeof(struct ether_header);
	ether_type = ntohs(ep->ether_type);

	printf("\n");

	if(ether_type <=1500) {
		;
	}
	else
	{
		if(eflag) {
			printf("\n\n ============== Datalink layer ===============\n\t");
			for(j=0; j<ETH_ALEN; j++) {
				printf("%X", ep->ether_dhost[j]);
				if(j!=5)printf(":");
			}
			printf("  -----> ");
			for(j=0; j<ETH_ALEN; j++) {
				printf("%X", ep->ether_shost[j]);
				if(j!=5) printf(":");
			}
			printf("\n\tether_type -> %x\n", ntohs(ep->ether_type));
		}

		iph = (struct iphdr *) p;
		i = 0;
		if (ntohs(ep->ether_type) == ETHERTYPE_IP){
			printf("\n\n ============= IP HEADER =============\n\t");
			printf("%s ----> ", inet_ntoa(iph->saddr));
			printf("%s\n", inet_ntoa(iph->daddr));
			printf("\tVersion:           %d\n", iph->version);
			printf("\tHeader Length:     %d\n", iph->ihl);
			printf("\tService:           %d\n", iph->tos);
			printf("\tTotal Length:      %d\n", ntohs(iph->tot_len));
			printf("\tIdentification:    %d\n", ntohs(iph->id));
			printf("\tFragment Offset:   %d\n", ntohs(iph->frag_off));
			printf("\tTime to Live:      %d\n", iph->ttl);
			printf("\tChecksum:          %d\n", ntohs(iph->check));

			if(iph->protocol == IPPROTO_TCP) {
				tcph = (struct tcphdr *) (p + iph->ihl *4);
				tcpdata = (unsigned char *) (p+(iph->ihl *4));
				printf("\n\n ============= TCP HEADER ============\n");
				printf("\tSource Port:                %d\n",ntohs(tcph->source));
				printf("\tDestination Port:           %d\n",ntohs(tcph->dest));
				printf("\tSequence Number:            %d\n",ntohl(tcph->seq));
				printf("\tAcknowledgement Number:     %d\n",ntohl(tcph->ack_seq));
				printf("\tData Offset:                %d\n",tcph->doff);
				printf("\tWindow:                     %d\n",ntohs(tcph->window));
				printf("\tURG: %d ACK: %d PSH:%d RST: %d SYN: %d FIN: %d\n",tcph->urg,tcph->ack,tcph->psh,tcph->rst,tcph->syn,tcph->fin,ntohs(tcph->check),ntohs(tcph->urg_ptr));
				printf("\n   ============== TCP DATA(HEXA) ============== \n\t");
				chcnt =0;
				for(temp = (iph->ihl *4) + (tcph->doff *4); temp<=ntohs(iph->tot_len)-1; temp++)
				{
					printf("%02x ", *(tcpdata++));
					if( (++chcnt %16)==0 ) printf("\n\t");
				}
				if(pflag) {
					printf("\n   ============= TCP DATA(CHAR) ==============\n\t");
					tcpdata= (unsigned char *) ((p+ iph->ihl*4) + (tcph->doff*4));
					for(temp = (iph->ihl *4) + (tcph->doff *4); temp<=ntohs(iph->tot_len)-1;temp++)
					printf("%c", *(tcpdata++));
				}
				printf("\n\t\t<<<< End of data >>>>>\n");
			}
			else if(iph->protocol ==IPPROTO_UDP) {
				udph = (struct udphdr *) (p+iph->ihl *4);
				udpdata = (unsigned char * )(p+iph->ihl*4)+8;
				printf("\n  ============ UDP HEADER =============\n");
				printf("\t Source Port :        %d\n",ntohs(udph->source));
				printf("\t Destination Port :   %d\n",ntohs(udph->dest));
				printf("\t Length :             %d\n",ntohs(udph->len));
				printf("\t Checksum :           %x\n",ntohs(udph->check));
				printf("\n  ==============  UDP DATA(HEXA) =============\n");
				chcnt = 0;
				for(temp = (iph->ihl*4)+8; temp<=ntohs(iph->tot_len)-1; temp++){
				printf("%02x ",*(udpdata++));
				if((++chcnt % 16)==0) printf("\n\t");
				}
				udpdata = (unsigned char *) (p + iph->ihl *4) +8;
				if(pflag)
				{
					printf("\n ============== UDP DATA(CHAR) ==============\n\t");
					for(temp = (iph->ihl*4)+8; temp<=ntohs(iph->tot_len)-1; temp++)
					printf("%c", *(udpdata++));
					}

				printf("\n\t\t<<<< End of Data >>>>>\n");
			}
			else if(iph->protocol == IPPROTO_ICMP) {
				icmph = (struct icmp * ) (p+iph->ihl *4);
				icmpdata = (unsigned char *) (p+iph->ihl*4)+8;
				printf("\n\n ============ ICMP HEADER =============\n");
				printf("\tType :                   %d\n", icmph->icmp_type);
				printf("\tCode :                   %d\n", icmph->icmp_code);
				printf("\tChecksum :                   %d\n", icmph->icmp_cksum);
				printf("\tID :                   %d\n", icmph->icmp_id);
				printf("\tSeq :                   %d\n", icmph->icmp_seq);
				chcnt = 0;
				for(temp = (iph->ihl *4) +8; temp<=ntohs(iph->tot_len)-1;temp++){
					printf("%02x ", *(icmpdata++));
					if( (++chcnt %16) ==0) printf("\n\t");
				}
				printf("\n\t\t<<<<<< End of Data >>>>>\n");
			}
		}
	}
}

void sig_int(int sig)
{
	printf("Bye!!\n");
	pcap_close(pd);
	close(sockfd);
	exit(0);
}

void usage(void)
{
	fprintf(stdout, "Usage : pa filter_rule [-pch]\n");
	fprintf(stdout, "         -p  : 데이타를 문자로 출력한다.\n");
	fprintf(stdout, "         -c  : 주어진 숫자만큼의 패킷만 덤프한다.\n");
	fprintf(stdout, "      -e  :  datalink layer 를 출력한다.\n");
	fprintf(stdout, "      -e  :  잡은 패킷을 생으로 찍는다.\n");
	fprintf(stdout, "         -h :  사용법\n");
}

int main(int argc, char *argv[])
{
	struct bpf_program fcode;
	pcap_handler printer;
	char ebuf[PCAP_ERRBUF_SIZE];
	int c,i,snaplen=512,size,packetcnt;
	bpf_u_int32 myself, localnet, netmask;
	unsigned char *pcap_userdata;

	filter_rule = argv[1];
	signal(SIGINT,sig_int);

	opterr =0;
	if(argc-1 <1)
	{
		usage();
		exit(1);
	}

	while( (c=getopt(argc,argv,"i:c:pher")) != -1) {
		switch(c) {
			case 'i' :
				device = optarg;
				break;
			case 'p' :
				pflag = 1;
				break;
			case 'c':
				cflag = 1;
				packetcnt = atoi(optarg);
				if(packetcnt <=0) {
					fprintf(stderr, "invalid pacet number %s",optarg);
					exit(1);
				}
				break;
			case 'e':
				eflag =1;
				break;
			case 'r':
				rflag =1;
				break;
			case 'h':
				usage();
				exit(1);
		}
	}

	if(device == NULL) {
		if( (device = pcap_lookupdev(ebuf) ) ==NULL)
		{
			perror(ebuf);
			exit(-1);
		}
	}
	fprintf(stdout,"device = %s\n", device);

	pd = pcap_open_live(device , snaplen, PROMISCOUS, 1000, ebuf);
	if(pd == NULL) {
		perror(ebuf);
		exit(-1);
	}

	i = pcap_snapshot(pd);
	if(snaplen <i) {
		perror(ebuf);
		exit(-1);
		}
	if(pcap_lookupnet(device, &localnet, &netmask, ebuf) <0) {
		perror(ebuf);
		exit(-1);
	}

	setuid(getuid());

	if(pcap_compile(pd, &fcode, filter_rule , 0, netmask)<0) {
		perror(ebuf);
		exit(-1);
	}

	if(pcap_setfilter(pd, &fcode) <0) {
		perror(ebuf);
		exit(-1);
	}

	fflush(stderr);

	printer = lookup_printer(pcap_datalink(pd));
	pcap_userdata = 0;
	if(pcap_loop(pd,packetcnt, printer, pcap_userdata) <0) {
		perror("pcap_loop error");
		exit(-1);
	}
	
	pcap_close(pd);
	exit(0);
}
				

