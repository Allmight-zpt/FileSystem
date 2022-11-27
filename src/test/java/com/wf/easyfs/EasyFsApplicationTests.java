package com.wf.easyfs;

//import okhttp3.*;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.io.*;
//import java.util.logging.Logger;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@Ignore
//public class EasyFsApplicationTests {

//    @Test
//    public void upload(){
//        OkHttpClient okHttpClient = new OkHttpClient();
//        File file1 = new File("C:\\Users\\ZPT\\Desktop\\pictureToUpload\\sanjiu2.jpg");
//        MultipartBody multipartBody = new MultipartBody.Builder()
//                //此处的photo数据名必须与请求接口接收时指定的名称一致！！！
//                .addFormDataPart("photo",file1.getName(), RequestBody.create(MediaType.parse("application/x-jpg"),file1))
//                .build();
//        Request request = new Request.Builder().url("http://localhost:8080/file/upload").post(multipartBody).build();
//        Call call = okHttpClient.newCall(request);
//        try {
//            Response response = call.execute();
//            System.out.println(response.body().string());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void download() throws Exception{
//        OkHttpClient okHttpClient = new OkHttpClient();
//        Request request = new Request.Builder().url("http://localhost:8080/file/2022/03/03/%E7%8B%97.jpg").build();
//        Call call = okHttpClient.newCall(request);
//        try {
//            Response response = call.execute();
//            String filename = "dog.jpg";
//            InputStream is = response.body().byteStream();
//            OutputStream os = new FileOutputStream(new File("C:\\Users\\ZPT\\Desktop\\pictureFromDownload\\"+filename));
//            byte[] buffer = new byte[1024 * 100];
//            int len;
//            while((len = is.read(buffer))!=-1){
//                os.write(buffer,0,len);
//            }
//            os.close();
//            is.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}

