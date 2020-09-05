package com.baidu.shop.base;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName Result
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/8/28
 * @Version V1.0
 **/
@Data //提供get,set
@NoArgsConstructor //提供无参构造
public class Result<T> {

    private Integer code;//返回码

    private String message;//返回消息

    private T data;//返回数据

    public Result(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = (T) data;
    }
}
