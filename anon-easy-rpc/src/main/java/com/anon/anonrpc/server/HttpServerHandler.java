package com.anon.anonrpc.server;

import com.anon.anonrpc.model.RpcRequest;
import com.anon.anonrpc.model.RpcResponse;
import com.anon.anonrpc.registry.LocalRegistry;
import com.anon.anonrpc.serializer.JdkSerializer;
import com.anon.anonrpc.serializer.Serializer;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * HTTP 请求处理器
 */
public class HttpServerHandler implements Handler<HttpServerRequest> {
    //    1.反序列化请求为对象，并从请求对象中获取参数。
    //    2.根据服务名称从本地注册器中获取到对应的服务实现类。
    //    3.通过反射机制调用方法，得到返回结果。
    //    4.对返回结果进行封装和序列化，并写入到响应中。
    @Override
    public void handle(HttpServerRequest request) {
        // 检查是否是浏览器直接访问
        if (request.method() == io.vertx.core.http.HttpMethod.GET) {
            // 返回友好的HTML页面，指定UTF-8编码
            request.response()
                   .putHeader("content-type", "text/html; charset=UTF-8")
                   .end("<html><body>" +
                        "<h1>RPC服务器正在运行</h1>" +
                        "<p>这是一个RPC服务端点，不支持直接浏览器访问。</p>" +
                        "<p>请使用正确的RPC客户端访问此服务。</p>" +
                        "</body></html>");
            return;
        }
        
        // 指定序列化器
        final Serializer serializer = new JdkSerializer();

        // 记录日志
        System.out.println("Recieved request:" + request.method() + " " + request.uri());

        // 异步处理HTTP请求
        request.bodyHandler(body -> {
            byte[] bytes = body.getBytes();
            RpcRequest rpcRequest = null;
            try {
                rpcRequest = serializer.deserialize(bytes, RpcRequest.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 构造响应结果对象
            RpcResponse rpcResponse = new RpcResponse();
            // 如果请求为null，直接返回
            if (rpcRequest == null) {
                rpcResponse.setMessage("rpcRequest is null");
                doResponse(request, rpcResponse, serializer);
                return;
            }
            try {
                // 获取要调用的服务实现类，通过反射调用
                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(implClass.newInstance(), rpcRequest.getArgs());
                // 封装返回结果
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            } catch (Exception e) {
                e.printStackTrace();
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }
            // 响应
            doResponse(request, rpcResponse, serializer);
        });
    }

    /**
     * 响应
     * @param request
     * @param rpcResponse
     * @param serializer
     */
    void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        HttpServerResponse httpServerResponse = request.response()
                .putHeader("content-type", "application/json");
        try {
            // 序列化
            byte[] serialized = serializer.serialize(rpcResponse);
            System.out.println("响应序列化成功，数据大小: " + serialized.length + " 字节");
            httpServerResponse.end(Buffer.buffer(serialized));
        } catch (IOException e) {
            System.err.println("序列化响应时出错: " + e.getMessage());
            e.printStackTrace();
            
            // 返回错误信息而不是空响应
            RpcResponse errorResponse = new RpcResponse();
            errorResponse.setMessage("服务器序列化错误: " + e.getMessage());
            
            try {
                // 尝试序列化错误响应
                byte[] errorBytes = serializer.serialize(errorResponse);
                httpServerResponse.end(Buffer.buffer(errorBytes));
            } catch (IOException ex) {
                // 如果连错误响应都无法序列化，发送纯文本错误
                httpServerResponse.putHeader("content-type", "text/plain")
                                 .end("服务器序列化错误");
            }
        }
    }
}
