#include <sys/time.h>//test
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

#define	 PROMISCUOUS 1

struct   iphdr    *iph;
struct   tcphdr  *tcph;
struct   udphdr *udph;
struct   icmp     *icmph;
static   pcap_t   *pd;
int sockfd;

int pflag;      // DATA를 문자로 찍을 것인지.
int rflag;      // DATA를 생으로 찍을 것인지.
int eflag;     // DATALINK layer print option
int cflag;     // 패킷을 이 순자만큼 찍어주고 종료한다.
int chcnt;    // 문자를 찍을 때 문자카운터 다음줄에 찍기위해

char	*device, *filter_rule;

void packet_analysis(unsigned char *, const struct pcap_pkthdr *, 
                    const unsigned char *);

struct printer {
   pcap_handler f;
   int type;
};
   
/* datalink type에 따른 불리어질 함수들의 
   목록들을 갖는 구조체                       
 Data-link level type codes. 
#define DLT_NULL		0	 no link-layer encapsulation 
#define DLT_EN10MB	1	 Ethernet (10Mb) 
#define DLT_EN3MB		2	 Experimental Ethernet (3Mb)
#define DLT_AX25		3	 Amateur Radio AX.25
#define DLT_PRONET	4	 Proteon ProNET Token Ring
#define DLT_CHAOS		5	 Chaos
#define DLT_IEEE802	6	 IEEE 802 Networks
#define DLT_ARCNET	7	 ARCNET
#define DLT_SLIP		8	 Serial Line IP
#define DLT_PPP		9	 Point-to-point Protocol
#define DLT_FDDI		10	 FDDI
#define DLT_ATM_RFC1483	11	 LLC/SNAP encapsulated atm
#define DLT_RAW		12	 raw IP
#define DLT_SLIP_BSDOS	13	 BSD/OS Serial Line IP
#define DLT_PPP_BSDOS	14	 BSD/OS Point-to-point Protocol
bpf.h 라는 헤더화일에 위와 같은 내용으로 정의되어 있다.		*/

static struct printer printers[] = {
   { packet_analysis, DLT_IEEE802 },
   { packet_analysis, DLT_EN10MB  },
   { NULL, 0 },
};
   
/*  datalink type에 따라 수행될 함수를 결정하게 된다.
    이는 pcap_handler라는 함수형 포인터의 값으로 대입된다. */
static pcap_handler lookup_printer(int type) 
{
	struct printer *p;
    
	for(p=printers; p->f; ++p)
   	    if(type == p->type)
	        return p->f;
	        
	perror("unknown data link type");
}


/* pcap_loop()에 의해 패킷을 잡을 때마다 불려지는 함수
   pcap_handler가 이 함수를 포인터하고 있기 때문이다 */
void packet_analysis(unsigned char *user, const struct pcap_pkthdr *h, 
                    const unsigned char *p)
{
    	int j, temp;
	unsigned int length = h->len;
	struct ether_header *ep;
	unsigned short ether_type;
	unsigned char *tcpdata, *udpdata,*icmpdata;
	register unsigned int i;
	
	chcnt = 0;
	
	// 잡은 패킷을 그대로 생으로 찍기	
	if(rflag) {
	    while(length--) {
	        printf("%02x ", *(p++));
	        if( (++chcnt % 16) == 0 ) printf("\n\t");
	    }
	    fprintf(stdout, "\n");
	    return;
	}

	length -= sizeof(struct ether_header);
	
	// ethernet header mapping
	ep = (struct ether_header *)p;
	// ethernet header 14 bytes를 건너 뛴 포인터
	p += sizeof(struct ether_header);
	// datalink type
	ether_type = ntohs(ep->ether_type);
	
	printf("\n");
	// lan frame이 IEEE802인경우 ether_type필드가 길이필드가 된다.
	if(ether_type <= 1500) {
	    ;
	    /*while(length--) {
		if(++is_llchdr <= 3) {
		    fprintf(stdout,"%02x",*p++);
		    continue;
		}
		if(++next_line == 16) {
		    next_line = 0;	
		    printf("\n\t");
		}
		printf("%02x",*p++);
	    }*/
	}
	else 
	{    
	    if(eflag) {
	    	printf("\n\n    =================== Datalink layer ===================\n\t");
	    	for(j=0; j<ETH_ALEN; j++){ 
		    printf("%X", ep->ether_dhost[j]); 
		    if(j != 5) printf(":");
	    	}
	    	printf("  ------> ");
	    	for(j=0; j<ETH_ALEN; j++) {
		    printf("%X", ep->ether_shost[j]);
	        	    if(j != 5) printf(":");
	    	}	
	    	printf("\n\tether_type -> %x\n", ntohs(ep->ether_type));
	    }

	    iph = (struct iphdr *) p;
	    i = 0;
	    if (ntohs(ep->ether_type) == ETHERTYPE_IP) {	// ip 패킷인가?
		// packet capturing한 것을 화면에 출력하는 부분
	        	printf("\n\n    ===================    IP HEADER   ===================\n");
		//printf("\t%s -----> ",   inet_ntoa(iph->saddr));
		//printf("%s\n", inet_ntoa(iph->daddr));
		printf("\tVersion:         %d\n", iph->version);
		printf("\tHerder Length:   %d\n", iph->ihl);
		printf("\tService:         %#x\n",iph->tos);
		printf("\tTotal Length:    %d\n", ntohs(iph->tot_len)); 
		printf("\tIdentification : %d\n", ntohs(iph->id));
		printf("\tFragment Offset: %d\n", ntohs(iph->frag_off)); 
		printf("\tTime to Live:    %d\n", iph->ttl);
		printf("\tChecksum:        %d\n", ntohs(iph->check));
	
		/* packet의 ip부분을 건너뛴 곳에서부터 tcp header의 시작이 된다.                            */
		if(iph->protocol == IPPROTO_TCP) {
		        tcph = (struct tcphdr *) (p + iph->ihl * 4);
		        // tcp data는 
		        tcpdata = (unsigned char *) (p + (iph->ihl*4) + (tcph->doff * 4));
	                        printf("\n\n    ===================   TCP HEADER   ===================\n");
	       	        printf("\tSource Port:              %d\n", ntohs(tcph->source));
		        printf("\tDestination Port:         %d\n", ntohs(tcph->dest));
		        printf("\tSequence Number:          %d\n", ntohl(tcph->seq));
		        printf("\tAcknowledgement Number:   %d\n", ntohl(tcph->ack_seq));
		        printf("\tData Offset:              %d\n", tcph->doff);
		        printf("\tWindow:                   %d\n", ntohs(tcph->window));
		        printf("\tURG:%d ACK:%d PSH:%d RST:%d SYN:%d FIN:%d\n", 
			tcph->urg, tcph->ack, tcph->psh, tcph->rst, 
			tcph->syn, tcph->fin, ntohs(tcph->check), 
			ntohs(tcph->urg_ptr));
		        printf("\n    ===================   TCP DATA(HEXA)  =================\n\t"); 
		        chcnt = 0;
		        for(temp = (iph->ihl * 4) + (tcph->doff * 4); temp <= ntohs(iph->tot_len) - 1; temp++) {
	   		    printf("%02x ", *(tcpdata++));
			    if( (++chcnt % 16) == 0 ) printf("\n\t");
		        }
		        if (pflag) {
			   printf("\n    ===================   TCP DATA(CHAR)  =================\n"); 
		                   tcpdata = (unsigned char *) ((p + iph->ihl*4) + (tcph->doff*4));
			   for(temp = (iph->ihl * 4) + (tcph->doff * 4); temp <= ntohs(iph->tot_len) - 1; temp++)
	   		        printf("%c", *(tcpdata++));
	                        }
		        printf("\n\t\t<<<<< End of Data >>>>>\n");
	        	}
		else if(iph->protocol == IPPROTO_UDP) {
	    	    udph = (struct udphdr *) (p + iph->ihl * 4);
		    udpdata = (unsigned char *) (p + iph->ihl*4) + 8;
		    printf("\n    ==================== UDP HEADER =====================\n");
		    printf("\tSource Port :      %d\n",ntohs(udph->source));
		    printf("\tDestination Port : %d\n", ntohs(udph->dest));
		    printf("\tLength :           %d\n", ntohs(udph->len));
	   	    printf("\tChecksum :         %x\n", ntohs(udph->check));
	            	    printf("\n    ===================  UDP DATA(HEXA)  ================\n\t");	 
		    chcnt = 0;
		    for(temp = (iph->ihl*4)+8; temp<=ntohs(iph->tot_len) -1; temp++) {
		       printf("%02x ", *(udpdata++));
		       if( (++chcnt % 16) == 0) printf("\n\t"); 
		    }

		    udpdata = (unsigned char *) (p + iph->ihl*4) + 8;
		    if(pflag) {
		        printf("\n===================  UDP DATA(CHAR)  ================\n");	 
		        for(temp = (iph->ihl*4)+8; temp<=ntohs(iph->tot_len) -1; temp++) 
		            printf("%c", *(udpdata++));
		    }
		    
		    printf("\n\t\t<<<<< End of Data >>>>>\n");
		}	  
		else if(iph->protocol == IPPROTO_ICMP) {
		        icmph = (struct icmp *) (p + iph->ihl * 4);
		        icmpdata = (unsigned char *) (p + iph->ihl*4) + 8;
	                        printf("\n\n    ===================   ICMP HEADER   ===================\n");
	       	        printf("\tType :                    %d\n", icmph->icmp_type);
		        printf("\tCode :                    %d\n", icmph->icmp_code);
		        printf("\tChecksum :                %02x\n", icmph->icmp_cksum);
		        printf("\tID :                      %d\n", icmph->icmp_id);
		        printf("\tSeq :                     %d\n", icmph->icmp_seq);
		        printf("\n    ===================   ICMP DATA(HEXA)  =================\n\t"); 
		        chcnt = 0;
		        for(temp = (iph->ihl * 4) + 8; temp <= ntohs(iph->tot_len) - 1; temp++) {
	   	            printf("%02x ", *(icmpdata++));
		            if( (++chcnt % 16) == 0 ) printf("\n\t");
		        }
		        printf("\n\t\t<<<<< End of Data >>>>>\n");
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
    fprintf(stdout," Usage : pa filter_rule [-pch]\n");
    fprintf(stdout,"         -p  :  데이타를 문자로 출력한다.\n");
    fprintf(stdout,"         -c  :  주어진 숫자만큼의 패킷만 덤프한다\n");
    fprintf(stdout,"	  -e  :  datalink layer를 출력한다.\n");
    fprintf(stdout,"	  -e  :  잡은 패킷을 생으로 찍는다.\n");
    fprintf(stdout,"         -h  :  사용법\n");
}

int main(int argc, char *argv[])
{
	struct	bpf_program fcode;
	pcap_handler printer;
	char	ebuf[PCAP_ERRBUF_SIZE];
	int	c, i, snaplen = 512, size, packetcnt;
	bpf_u_int32 myself, localnet, netmask;
	unsigned char	*pcap_userdata;
		
	filter_rule = argv[1];		// ex) src host xxx.xxx.xxx.xxx and tcp port 80
	
	signal(SIGINT,sig_int);	// signal hanlder 등록 
	
	opterr = 0;
	
	if(argc-1 < 1) {		// option check
	    usage(); 
	    exit(1);
	}
	
	while( (c = getopt(argc, argv,"i:c:pher")) != -1) {
	    switch(c) {
	    	case 'i'  :			// 패킷 캡쳐 기기 지정 
	    	        device = optarg;	
	    	        break;
	    	case 'p' : 		// 데이터를 문자로 출력하는 옵션
		        pflag = 1; 
		        break;
	    	case 'c' : 		// 덤프하려는 패킷의 수
		        cflag = 1; 
		        packetcnt = atoi(optarg);
		        if(packetcnt <= 0) {
			fprintf(stderr,"invalid pacet number %s",optarg);
			exit(1);
		        }
		        break;
		case 'e' :      		// 데이터링크 계층 출력
		        eflag = 1;
		        break;		
		case 'r' :      		// 잡은 패킷을 맵핑없이 16진수로 모두 찍는다.
		        rflag = 1;
		        break;		
	    	case 'h' :			// 사용법
		        usage();
		        exit(1);
	    }
	}	    
	
	if (device == NULL ) {
	    if ( (device = pcap_lookupdev(ebuf) ) == NULL) {
	        perror(ebuf);           
   	        exit(-1);
   	    }
   	}
   	fprintf(stdout, "device = %s\n", device);
	
	pd = pcap_open_live(device, snaplen, PROMISCUOUS, 1000, ebuf);
	if(pd == NULL) {
   	    perror(ebuf);          
   	    exit(-1);
   	}
	
	i = pcap_snapshot(pd);
	if(snaplen < i) {
   	    perror(ebuf);                            
  	    exit(-1);
   	}
	
	if(pcap_lookupnet(device, &localnet, &netmask, ebuf) < 0) {
   	    perror(ebuf);
   	    exit(-1);
   	}
	
	setuid(getuid());
	
	if(pcap_compile(pd, &fcode, filter_rule, 0, netmask) < 0) {
   	    perror(ebuf);
   	    exit(-1);
   	}
	
	if(pcap_setfilter(pd, &fcode) < 0) {
   	    perror(ebuf);
   	    exit(-1);
   	}
	
	fflush(stderr);
	
	printer = lookup_printer(pcap_datalink(pd));
	pcap_userdata = 0;
	
	if(pcap_loop(pd, packetcnt, printer, pcap_userdata) < 0) {
   	    perror("pcap_loop error");
   	    exit(-1);
   	}
	
	pcap_close(pd);
	exit(0);
}

