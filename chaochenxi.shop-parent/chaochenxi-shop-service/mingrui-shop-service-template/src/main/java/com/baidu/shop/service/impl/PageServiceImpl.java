package com.baidu.shop.service.impl;

import com.baidu.shop.base.Result;
import com.baidu.shop.dto.*;
import com.baidu.shop.entity.*;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.service.PageService;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName PageServiceImpl
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/23
 * @Version V1.0
 **/
//@Service
public class PageServiceImpl implements PageService {

    //@Autowired
    private BrandFeign brandFeign;

    //@Autowired
    private GoodsFeign goodsFeign;

    //@Autowired
    private CategoryFeign categoryFeign;

    //@Autowired
    private SpecificationFeign specificationFeign;

    //@Override
    public Map<String, Object> getPageInfoBySpuId(Integer spuId) {

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
                Result<List<CategoryEntity>> categoryResult = categoryFeign.getCategoryByIdList(String.join(",",Arrays.asList(spuInfo.getCid1() + "", spuInfo.getCid2() + "", spuInfo.getCid3() + "")));
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
