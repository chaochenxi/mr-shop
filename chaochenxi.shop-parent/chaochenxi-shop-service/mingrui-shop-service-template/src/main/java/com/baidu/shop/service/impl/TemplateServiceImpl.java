package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.*;
import com.baidu.shop.entity.*;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.service.TemplateService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @ClassName TemplateServiceImpl
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/25
 * @Version V1.0
 **/
@RestController
public class TemplateServiceImpl extends BaseApiService implements TemplateService {

    @Autowired
    private BrandFeign brandFeign;

    @Autowired
    private GoodsFeign goodsFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Autowired
    private SpecificationFeign specificationFeign;

    @Value(value = "${mrshop.static.html.path}")
    private String staticHTMLPath;

    //注入静态化模版
    @Autowired
    private TemplateEngine templateEngine;

    @Override
    public Result<JSONObject> createStaticHTMLTemplate(Integer spuId) {

        //也就是说我们现在可以创建上下文了
        Map<String, Object> map = this.getPageInfoBySpuId(spuId);
        //创建模板引擎上下文
        Context context = new Context();
        //将所有准备的数据放到模板中
        context.setVariables(map);

        File file = new File(staticHTMLPath, spuId + ".html");
        //构建文件输出流
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file, "UTF-8");
            templateEngine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }finally {
            writer.close();
        }

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> initStaticHTMLTemplate() {

        //获取所有spu数据
        Result<List<SpuDTO>> spuInfoResult = goodsFeign.getSpuInfo(new SpuDTO());
        if (spuInfoResult.getCode() == 200) {
            List<SpuDTO> spuDTOList = spuInfoResult.getData();
            spuDTOList.stream().forEach(spuDTO -> {

                createStaticHTMLTemplate(spuDTO.getId());
            });
        }

        return this.setResultSuccess();
    }

    private Map<String, Object> getPageInfoBySpuId(Integer spuId) {

        Map<String, Object> map = new HashMap<>();

        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);

        Result<List<SpuDTO>> spuInfoResult = goodsFeign.getSpuInfo(spuDTO);

        if(spuInfoResult.getCode() == 200){
            if (spuInfoResult.getData().size() == 1) {
                //spu信息
                SpuDTO spuInfo = spuInfoResult.getData().get(0);
                map.put("spuInfo",spuInfo);
                //品牌信息
                BrandDTO brandDTO = new BrandDTO();
                brandDTO.setId(spuInfo.getBrandId());
                Result<PageInfo<BrandEntity>> brandInfoResult = brandFeign.getBrandInfo(brandDTO);
                if(brandInfoResult.getCode() == 200){
                    PageInfo<BrandEntity> data = brandInfoResult.getData();

                    List<BrandEntity> brandList = data.getList();
                    if(brandList.size() == 1){
                        map.put("brandInfo",brandList.get(0));
                    }
                }

                //分类信息
                Result<List<CategoryEntity>> categoryResult = categoryFeign.getCategoryByIdList(String.join(",", Arrays.asList(spuInfo.getCid1() + "", spuInfo.getCid2() + "", spuInfo.getCid3() + "")));
                if (categoryResult.getCode() == 200){
                    List<CategoryEntity> categoryEntityList = categoryResult.getData();
                    map.put("categoryList",categoryEntityList);
                }

                //sku
                Result<List<SkuDTO>> skusResult = goodsFeign.getSkuBySpuId(spuInfo.getId());
                if(skusResult.getCode() == 200){

                    List<SkuDTO> skuList = skusResult.getData();
                    map.put("skus",skuList);
                }

                //特有规格参数
                SpecParamDTO specParamDTO = new SpecParamDTO();
                specParamDTO.setCid(spuInfo.getCid3());
                specParamDTO.setGeneric(false);
                Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
                if(specParamResult.getCode() == 200) {
                    //需要将数据转换为map方便页面操作!!!!!!!!!
                    Map<Integer, String> specMap = new HashMap<>();
                    specParamResult.getData().stream().forEach(spec -> specMap.put(spec.getId(),spec.getName()));
                    map.put("specParamMap",specMap);
                }

                //spuDetail
                Result<SpuDetailEntity> spuDetailBydSpu = goodsFeign.getSpuDetailBydSpu(spuInfo.getId());
                if (spuDetailBydSpu.getCode() == 200){
                    SpuDetailEntity spuDetailInfo = spuDetailBydSpu.getData();
                    map.put("spuDetailInfo",spuDetailInfo);
                }

                //规格组和规格参数
                SpecGroupDTO specGroupDTO = new SpecGroupDTO();
                specGroupDTO.setCid(spuInfo.getCid3());
                Result<List<SpecGroupEntity>> specGroupResult = specificationFeign.getSpecGroupInfo(specGroupDTO);
                if (specGroupResult.getCode() == 200) {
                    List<SpecGroupEntity> specGroupInfo = specGroupResult.getData();
                    //规格组和规格参数
                    List<SpecGroupDTO> groupsInParams = specGroupInfo.stream().map(specGroup -> {

                        SpecGroupDTO specgroup = BaiduBeanUtil.copyProperties(specGroup, SpecGroupDTO.class);
                        //规格参数-通用参数
                        SpecParamDTO paramDTO = new SpecParamDTO();
                        paramDTO.setGroupId(specGroup.getId());
                        paramDTO.setGeneric(true);
                        Result<List<SpecParamEntity>> specParam = specificationFeign.getSpecParamInfo(specParamDTO);

                        if (specParam.getCode() == 200) {
                            specgroup.setParamList(specParam.getData());
                        }

                        return specgroup;
                    }).collect(Collectors.toList());

                    map.put("groupsInParams",groupsInParams);
                }

            }
        }

        return map;
    }

}
