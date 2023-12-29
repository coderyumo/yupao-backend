package com.yupi.yupaobackend.common;

import lombok.Data;

/**
 * @description:
 * @author: linli
 * @create: 2023-12-29 15:06
 **/
@Data
public class PageRequest {


    /**
     * 页量
     */
    protected int pageSize = 10;

    /**
     * 当前页
     */
    protected int pageNum = 1;


}
