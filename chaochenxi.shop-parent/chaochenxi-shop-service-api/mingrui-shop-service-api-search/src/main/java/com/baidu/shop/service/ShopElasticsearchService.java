package com.baidu.shop.service;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.Result;
import com.baidu.shop.document.GoodsDocument;
import com.baidu.shop.response.GoodsResponse;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @ClassName ShopElasticsearchService
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/16
 * @Version V1.0
 **/
@Api(tags = "es接口")
public interface ShopElasticsearchService {

    //ES数据初始化-->索引创建,映射创建,mysql数据同步
    @ApiOperation(value = "ES商品数据初始化-->索引创建,映射创建,mysql数据同步")
    @GetMapping(value = "es/initGoodsEsData")
    Result<JSONObject> initEsData();

    @ApiOperation(value = "清空ES中的商品数据")
    @GetMapping(value = "es/clearGoodsEsData")
    Result<JSONObject> clearEsData();

    @ApiOperation(value = "查询数据")
    @GetMapping(value = "es/search")
    GoodsResponse search(@RequestParam String search, @RequestParam Integer page);

}
