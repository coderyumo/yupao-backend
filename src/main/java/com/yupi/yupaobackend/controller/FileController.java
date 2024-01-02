package com.yupi.yupaobackend.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.yupi.yupaobackend.common.BaseResponse;
import com.yupi.yupaobackend.common.ResultUtils;
import com.yupi.yupaobackend.model.domain.Avatar;
import com.yupi.yupaobackend.service.AvatarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * @description: 文件上传相关接口
 * @author: linli
 * @create: 2024-01-02 16:47
 **/
@RestController
@RequestMapping("/file")
@CrossOrigin(origins = {"http://user.code-li.fun","http://yupao.code-li.fun"})
@Slf4j
public class FileController {

    @Value("${avatar.upload.filePath}")
    public String filePath;

    @Resource
    private AvatarService avatarService;


    /**
     * 文件上传接口
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> upload(@RequestParam MultipartFile file) throws Exception {

        String originalFilename = file.getOriginalFilename();
        String type = FileUtil.extName(originalFilename);
        long size = file.getSize();
        // 定义保存文件的目录
        File avatarParent = new File(filePath);

        if (!avatarParent.exists()) {
            avatarParent.mkdirs();
        }

        // 定义文件的唯一标识符
        String uuid = IdUtil.fastSimpleUUID();
        String fileUuid = uuid + "." + type;
        File avatar = new File(filePath + File.separator + fileUuid);
        // 将文件保存到指定位置
        file.transferTo(avatar);
        String md5 = SecureUtil.md5(avatar);
        //获取文件的md5
        md5 = SecureUtil.md5(avatar);
        //查询文件是否存在
        QueryWrapper<Avatar> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Avatar::getMd5, md5);
        List<Avatar> avatarList = avatarService.list(queryWrapper);

        //根据名字查询文件是否存在
        QueryWrapper<Avatar> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Avatar::getName, originalFilename);
        Avatar serviceOne = avatarService.getOne(wrapper);
        if (serviceOne != null){
            avatar.delete();
            return ResultUtils.success(serviceOne.getUrl());
        }
        String url = "";
        if (CollectionUtils.isNotEmpty(avatarList)) {
            url = avatarList.get(0).getUrl();
            avatar.delete();
        }else {
            url = "http://yupao-backend.code-li.fun/api/file/" + fileUuid;
        }

        //存到数据库
        Avatar avatarFile = new Avatar();
        avatarFile.setName(originalFilename);
        avatarFile.setSize(size / 1024);
        avatarFile.setType(type);
        avatarFile.setUrl(url);
        avatarFile.setMd5(md5);
        avatarFile.setUserId(Long.MAX_VALUE);
        avatarService.save(avatarFile);
        // 返回文件路径或其他响应
        return ResultUtils.success(url);
    }

    @GetMapping("/{fileUUID}")
    public void down(@PathVariable String fileUUID, HttpServletResponse response) throws IOException {
        File file = new File(filePath + "\\" + fileUUID);
        ServletOutputStream os = response.getOutputStream();
        response.addHeader("Content-Disposition", "attachment;filename" + URLEncoder.encode(fileUUID, "UTF-8"));
        response.setContentType("application/octet-stream");
        os.write(jodd.io.FileUtil.readBytes(file));
        os.flush();
        os.close();
    }
}
