import java.net.*;
import java.util.Random;

public class UDPPing {
    public static void main(String[] args) {
        if (args.length != 2 && args.length != 5) {
            System.out.println("Usage: UDPPing <dest_ip> <dest_port>");
            System.out.println("Usage: UDPPing <dest_ip> <dest_port> <INTERVAL=ms> <PINGCOUNT> <LEN=bytes>");
            return;
        }

        UDPPing ping = new UDPPing(args[0],  (short)Integer.parseInt(args[1]));

        if (args.length == 5) {
            ping.SetINTERVAL(Integer.parseInt(args[2])).SetPINGCOUNT(Integer.parseInt(args[3])).SetLEN(Integer.parseInt(args[4]));
        }
        else{
            ping.SetINTERVAL(100).SetPINGCOUNT(100).SetLEN(64);
        }
        
        ping.AsyncPing();
        try{
           //sleep 2sec
           Thread.sleep(2000);
        }
        catch(InterruptedException e){
        }
        //stop ping
        ping.AyncStopPing();
        System.out.println("Rtt: " + ping.GetRtt() + "ms");
        System.out.println("MaxRtt: " + ping.GetMaxRtt() + "ms");
        System.out.println("MinRtt: " + ping.GetMinRtt() + "ms");
        System.out.println("LossRate: " + ping.GetLossRate());
    }    

    public int INTERVAL = 100; // ping 间隔
    public int PINGCOUNT = 100;  // ping 次数
    public int LEN = 64; //ping 数据长度
    
    private int mCountSend = 0;
    private int mCountRecieve = 0;
    private double mRttSum = 0.0;
    private double mRttMin = 99999999.0;
    private double mRttMax = 0.0;

    private DatagramSocket m_Socket = null;
    private String m_Ip = "";
    private short m_Port = 0;

    Thread m_asyncThread;

    public UDPPing SetINTERVAL(int interval){
        this.INTERVAL = interval;
        return this;
    }

    public UDPPing SetPINGCOUNT(int pingcount){
        this.PINGCOUNT = pingcount;
        return this;
    }

    public UDPPing SetLEN(int len ){
        this.LEN = len;
        return this;
    }

    public double GetRtt() {
        if(mCountRecieve == 0) return 99999999.0;
        return mRttSum / mCountRecieve;
    }

    public double GetMaxRtt(){
        return mRttMax;
    }

    public double GetMinRtt(){
        return mRttMin;
    }

    public double GetLossRate(){
        if(mCountSend == 0) return 1.0f;
        return (mCountSend - mCountRecieve) / mCountSend;
    }

    public UDPPing(String ip, short port) {
        this.m_Ip = ip;
        this.m_Port = port;

    }

    private synchronized void decrement() {
        PINGCOUNT--;
    }

    public void Ping(){
        int count = PINGCOUNT;
        try {
            m_Socket = new DatagramSocket();
            m_Socket.setSoTimeout(INTERVAL*10);
        } catch (SocketException e) {
            m_Socket = null;
            return;
        }

        while (count >= 0) {
            decrement();
            count = PINGCOUNT;

            byte[] buffer = new byte[LEN];
            new Random().nextBytes(buffer);
            DatagramPacket packet  = null; 
            long timeOfSend = System.currentTimeMillis();
            try {
                mCountSend ++;
                packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(m_Ip), m_Port);
                m_Socket.send(packet);
            } catch (Exception e) {
                break;
            }

            try {
                m_Socket.receive(packet);
                long rtt = System.currentTimeMillis() - timeOfSend;
                mRttSum += rtt;
                mCountRecieve ++;
                mRttMin = Math.min(rtt, mRttMin);
                mRttMax = Math.max(rtt, mRttMax);
            } catch (SocketTimeoutException ignored) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        if(m_Socket != null) m_Socket.close();
    }


    public void AsyncPing() {
        m_asyncThread = new Thread(() -> {
            Ping();
        });
        m_asyncThread.start();
    }

    public void AyncStopPing(){
        if(m_asyncThread == null)   
            return;
        while (PINGCOUNT >= 0) {
            decrement();
        }
        try{
            m_asyncThread.join();
        }
        catch (InterruptedException e){
        }
    }
}