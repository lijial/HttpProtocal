package com.heyjl.httpprotocal;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;


public class HttpServer {
    public static void main(String[] args) throws Exception {
        // 创建serversocketchannel
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 监听8080端口
        ssc.socket().bind(new InetSocketAddress(8080));
        // 设置为非阻塞模式
        ssc.configureBlocking(false);
        // 向ssc注册选择器
        Selector selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        // 创建处理器
        while (true) {
            // 等待请求，每次等待阻塞3s，超过3s后线程继续向下执行
            if (selector.select(3000) == 0) {
                continue;
            } else {
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    // 启动新线程处理SelectionKey
                    new Thread(new HttpHandler(key)).run();
                    // 处理完成后，从待处理的SelectionKey迭代器中移除当前使用的key
                    keyIter.remove();
                }
            }
        }
    }


    private static class HttpHandler implements Runnable {
        private int bufferSize = 1024;
        private String localCharset = "UTF-8";
        private SelectionKey key;


        public HttpHandler(SelectionKey key) {
            this.key = key;
        }


        public void handleAccept() throws Exception {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }


        public void handleRead() throws Exception {
            // 获取channel
            SocketChannel sc = (SocketChannel) key.channel();
            // 获取buffer并充值
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            // 没有读到内容则关闭
            if (sc.read(buffer) == -1) {
                sc.close();
            } else {
                // 接收请求数据
                buffer.flip();
                String receivedString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();
                String[] requestMessage = receivedString.split("\r\n");
                for(String msg:requestMessage){
                    System.out.println(msg);
                    //如果遇到空说明报文头已经打印完
                    if(msg.isEmpty()){
                        break;
                    }
                }
                StringBuilder sendString = new StringBuilder();
                //响应报文首行
                sendString.append("HTTP/1.1 200 OK\r\n");
                sendString.append("Content-Type:text/html;charset="+localCharset + "\r\n");
                sendString.append("\r\n");
                sendString.append("接收的报文到的请求报文是：");
                for(String msg:requestMessage){
                    sendString.append(msg+"");
                }
                sendString.append("");
                buffer = ByteBuffer.wrap(sendString.toString().getBytes(localCharset));
                sc.write(buffer);
                sc.close();
            }
        }


        public void run() {
            try {
                //接收并连接到请求时
                if(key.isAcceptable()){
                    handleAccept();
                }
                //读数据时
                if(key.isReadable()){
                    handleRead();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }


    }
}
