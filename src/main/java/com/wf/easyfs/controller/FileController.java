package com.wf.easyfs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import javax.imageio.stream.FileImageInputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件服务器
 * create by 朱培挺
 */
// 允许跨域访问
@CrossOrigin
@Controller
@Slf4j
public class FileController {
    //定义全局变量，直接从yaml文件中注入
    @Value("${fs.dir}")
    private String fileDir;
    @Value("${fs.uuidName}")
    private Boolean uuidName;
    @Value("${fs.useSm}")
    private Boolean useSm;
    @Value("${fs.useNginx}")
    private Boolean useNginx;
    @Value("${fs.nginxUrl}")
    private String nginxUrl;

    // 首页
    @RequestMapping({"/", "/index"})
    public String index() {
        return "index.html";
    }

    /**
     * 上传文件
     */
    @ResponseBody
    @PostMapping("/file/upload")
    // 要点1. 此处的RequestParam参数名file必须与前端请求时的名字一致才能接收！！！
    public Map upload(@RequestParam("file") MultipartFile file) {
        if (fileDir == null) {
            fileDir = "/";
        }
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        // 文件原始名称
        String originalFileName = file.getOriginalFilename();
        // 获取前缀后缀
        String suffix = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        String prefix = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        // 要点2. 保存到磁盘 -> 基本思路就是在服务器打开一个新文件 然后把接收到的数据写入
        File outFile;
        String path;
        // 暂时不考虑uuid
        if (uuidName != null && uuidName) {
            path = getDate() + UUID.randomUUID().toString().replaceAll("-", "") + "." + suffix;
            outFile = new File(fileDir + path);
        } else {
            // 1. 首先文件存储路径是使用上传日期（年月日）进行确定的
            // 2. 其次要解决文件名重复的问题 -> 此处的思路十分巧妙 将名字后的数字不断＋1 直到没有重名文件（要点3）
            int index = 1;
            path = getDate() + originalFileName;
            outFile = new File(fileDir + path);
            while (outFile.exists()) {
                path = getDate() + prefix + "[" + index + "]." + suffix;
                outFile = new File(fileDir + path);
                index++;
            }
        }
        Map rs = null;
        try {
            // 如果日期定位的路径不存在则要创建文件夹
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            // 把数据写入新建的文件中
            file.transferTo(outFile);
            rs = getRS(200, "上传成功", path);
            try{
                //生成缩略图
                if (useSm != null && useSm) {
                    // 获取文件类型
                    String contentType = null;
                    try {
                        contentType = Files.probeContentType(Paths.get(outFile.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (contentType != null && contentType.startsWith("image/")) {
                        File smImg = new File(fileDir + "sm/" + path);
                        if (!smImg.getParentFile().exists()) {
                            smImg.getParentFile().mkdirs();
                        }
                        Thumbnails.of(outFile).scale(1f).outputQuality(0.25f).toFile(smImg);
                        rs.put("smUrl", "sm/" + path);
                    }
                }
                return rs;
            }catch (Exception e){
                return rs;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return getRS(500, e.getMessage());
        }
    }

    /**
     * 查看原文件
     */
    // 要点4. 提取url中信息的方法！
    @GetMapping("/file/{y}/{m}/{d}/{file:.+}")
    public String file(@PathVariable("y") String y, @PathVariable("m") String m, @PathVariable("d") String d, @PathVariable("file") String filename, HttpServletResponse response) throws UnsupportedEncodingException {
        String filePath = y + "/" + m + "/" + d + "/" + filename;
        if (useNginx) {
            if (nginxUrl == null) {
                nginxUrl = "/";
            }
            if (!nginxUrl.endsWith("/")) {
                nginxUrl += "/";
            }
            String newName;
            try {
                newName = URLEncoder.encode(filePath, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                newName = filePath;
            }
            return "redirect:" + nginxUrl + newName;
        }
        if (fileDir == null) {
            fileDir = "/";
        }
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        outputFile(fileDir + filePath, response);
        return null;
    }

    /**
     * 查看缩略图
     */
    @GetMapping("/file/sm/{y}/{m}/{d}/{file:.+}")
    public String fileSm(@PathVariable("y") String y, @PathVariable("m") String m, @PathVariable("d") String d, @PathVariable("file") String filename, HttpServletResponse response) throws UnsupportedEncodingException {
        String filePath = "sm/" + y + "/" + m + "/" + d + "/" + filename;
        if (useNginx) {
            if (nginxUrl == null) {
                nginxUrl = "/";
            }
            if (!nginxUrl.endsWith("/")) {
                nginxUrl += "/";
            }
            String newName;
            try {
                newName = URLEncoder.encode(filePath, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                newName = filePath;
            }
            return "redirect:" + nginxUrl + newName;
        }
        if (fileDir == null) {
            fileDir = "/";
        }
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        outputFile(fileDir + filePath, response);
        return null;
    }

    // 输出文件流
    private void outputFile(String file, HttpServletResponse response) throws UnsupportedEncodingException {
        // 判断文件是否存在
        File inFile = new File( file);
        if (!inFile.exists()) {
            PrintWriter writer = null;
            try {
                response.setContentType("text/html;charset=UTF-8");
                writer = response.getWriter();
                writer.write("<!doctype html><title>404 Not Found</title><h1 style=\"text-align: center\">404 Not Found</h1><hr/><p style=\"text-align: center\">Easy File Server</p>");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // 获取文件类型
        Path path = Paths.get(inFile.getName());
        String contentType = null;
        try {
            // 要点5. 获取文件contentType的方法
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 要点6. contentType 不为空时直接输出展示
        // contentType为空的时候就设置为强制下载，必须下载才能打开
        // 下载设置Content-Disposition即可
        if (contentType != null) {
            response.setContentType(contentType);
            // 下载图片！！！！！加入新功能
            // 注意！对于java调用下载图片而言不需要这一句，直接接收返回的数据即可，因为java是我们自己写的我们知道返回的数据
            // 我们不需要通过这个字段进行进一步的判断（当然也可以需要，但简单起见没写）
            // 这一句是写给浏览器看的，浏览器会做出相应的动作
            //response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(inFile.getName(), "utf-8"));

        } else {
            response.setContentType("application/force-download");
            String newName;
            try {
                newName = URLEncoder.encode(inFile.getName(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                newName = inFile.getName();
            }
            response.setHeader("Content-Disposition", "attachment;fileName=" + newName);
        }
        // 输出文件流
        OutputStream os = null;
        FileInputStream is = null;
        try {
            is = new FileInputStream(inFile);
            os = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取全部文件
     */
    @ResponseBody
    @RequestMapping("/api/list")
    public Map list(String dir, String accept, String exts) {
        String[] mExts = null;
        if (exts != null && !exts.trim().isEmpty()) {
            mExts = exts.split(",");
        }
        if (fileDir == null) {
            fileDir = "/";
        }
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        Map<String, Object> rs = new HashMap<>();
        if (dir == null || "/".equals(dir)) {
            dir = "";
        } else if (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        File file = new File(fileDir + dir);
        File[] listFiles = file.listFiles();
        List<Map> dataList = new ArrayList<>();
        if (listFiles != null) {
            for (File f : listFiles) {
                if ("sm".equals(f.getName())) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>();
                m.put("name", f.getName());  // 文件名称
                m.put("updateTime", f.lastModified());  // 修改时间
                m.put("isDir", f.isDirectory());  // 是否是目录
                if (f.isDirectory()) {
                    m.put("type", "dir");  // 文件类型
                } else {
                    String type;
                    m.put("url", (dir.isEmpty() ? dir : (dir + "/")) + f.getName());  // 文件地址
                    // 获取文件类型
                    Path path = Paths.get(f.getName());
                    String contentType = null;
                    String suffix = f.getName().substring(f.getName().lastIndexOf(".") + 1);
                    try {
                        contentType = Files.probeContentType(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 筛选文件类型
                    if (accept != null && !accept.trim().isEmpty() && !accept.equals("file")) {
                        if (contentType == null || !contentType.startsWith(accept + "/")) {
                            continue;
                        }
                        if (mExts != null) {
                            for (String ext : mExts) {
                                if (!f.getName().endsWith("." + ext)) {
                                    continue;
                                }
                            }
                        }
                    }
                    // 获取文件图标
                    if ("ppt".equalsIgnoreCase(suffix) || "pptx".equalsIgnoreCase(suffix)) {
                        type = "ppt";
                    } else if ("doc".equalsIgnoreCase(suffix) || "docx".equalsIgnoreCase(suffix)) {
                        type = "doc";
                    } else if ("xls".equalsIgnoreCase(suffix) || "xlsx".equalsIgnoreCase(suffix)) {
                        type = "xls";
                    } else if ("pdf".equalsIgnoreCase(suffix)) {
                        type = "pdf";
                    } else if ("html".equalsIgnoreCase(suffix) || "htm".equalsIgnoreCase(suffix)) {
                        type = "htm";
                    } else if ("txt".equalsIgnoreCase(suffix)) {
                        type = "txt";
                    } else if ("swf".equalsIgnoreCase(suffix)) {
                        type = "flash";
                    } else if ("zip".equalsIgnoreCase(suffix) || "rar".equalsIgnoreCase(suffix) || "7z".equalsIgnoreCase(suffix)) {
                        type = "zip";
                    } else if ("zip".equalsIgnoreCase(suffix) || "rar".equalsIgnoreCase(suffix) || "7z".equalsIgnoreCase(suffix)) {
                        type = "zip";
                    } else if (contentType != null && contentType.startsWith("audio/")) {
                        type = "mp3";
                    } else if (contentType != null && contentType.startsWith("video/")) {
                        type = "mp4";
                    }/* else if (contentType != null && contentType.startsWith("image/")) {
                        type = "file";
                        m.put("hasSm", true);
                        m.put("smUrl", m.get("url"));  // 缩略图地址
                    }*/ else {
                        type = "file";
                    }
                    m.put("type", type);
                    // 是否有缩略图
                    String smUrl = "sm/" + (dir.isEmpty() ? dir : (dir + "/")) + f.getName();
                    if (new File(fileDir + smUrl).exists()) {
                        m.put("hasSm", true);
                        m.put("smUrl", smUrl);  // 缩略图地址
                    }
                }
                dataList.add(m);
            }
        }
        // 根据上传时间排序
        Collections.sort(dataList, new Comparator<Map>() {
            @Override
            public int compare(Map o1, Map o2) {
                Long l1 = (long) o1.get("updateTime");
                Long l2 = (long) o2.get("updateTime");
                return l1.compareTo(l2);
            }
        });
        // 把文件夹排在前面
        Collections.sort(dataList, new Comparator<Map>() {
            @Override
            public int compare(Map o1, Map o2) {
                Boolean l1 = (boolean) o1.get("isDir");
                Boolean l2 = (boolean) o2.get("isDir");
                return l2.compareTo(l1);
            }
        });
        rs.put("code", 200);
        rs.put("msg", "查询成功");
        rs.put("data", dataList);
        return rs;
    }

    /**
     * 删除
     */
    @ResponseBody
    @RequestMapping("/api/del")
    public Map del(String file) {
        if (fileDir == null) {
            fileDir = "/";
        }
        if (!fileDir.endsWith("/")) {
            fileDir += "/";
        }
        if (file != null && !file.isEmpty()) {
            File f = new File(fileDir + file);
            if (f.delete()) {
                File smF = new File(fileDir + "sm/" + file);
                smF.delete();
                return getRS(200, "删除成功");
            }
        }
        return getRS(500, "删除失败");
    }

    // 获取当前日期
    private String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        return sdf.format(new Date());
    }

    // 封装返回结果
    private Map getRS(int code, String msg, String url) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("msg", msg);
        if (url != null) {
            map.put("url", url);
        }
        return map;
    }

    // 封装返回结果
    private Map getRS(int code, String msg) {
        return getRS(code, msg, null);
    }


}
