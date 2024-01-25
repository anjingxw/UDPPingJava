# UDPPingJava
ping with UDP protocol 🛠

# Server
```
socat -v UDP-LISTEN:4000,fork PIPE
Now a echo server is listening at port 4000.

```
# 编译
```
javac UDPPing.java
```

# 执行
```
java UDPPing 127.0.0.1 4000
java UDPPing 127.0.0.1 4000 100 100 64
```