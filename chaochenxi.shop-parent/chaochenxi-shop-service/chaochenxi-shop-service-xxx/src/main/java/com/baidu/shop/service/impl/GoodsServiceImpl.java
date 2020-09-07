package com.baidu.shop.service.impl;

import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpuEntity;
import com.baidu.shop.mapper.CategoryMapper;
import com.baidu.shop.mapper.SpuMapper;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.netflix.discovery.converters.Auto;
import com.netflix.ribbon.proxy.annotation.Http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @ClassName GoodsServiceImpl
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/7
 * @Version V1.0
 **/
@RestController
public class GoodsServiceImpl extends BaseApiService implements GoodsService {

    @Resource
    private SpuMapper spuMapper;

    @Autowired
    private BrandService brandService;

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public Result<Map<String, Object>> getSpuInfo(SpuDTO spuDTO) {

        //分页
        if (ObjectUtil.isNotNull(spuDTO.getPage()) && ObjectUtil.isNotNull(spuDTO.getRows())){
            PageHelper.startPage(spuDTO.getPage(),spuDTO.getRows());
        }

        //构建条件查询
        Example example = new Example(SpuEntity.class);

        //构建查询条件
        Example.Criteria criteria = example.createCriteria();
        if(StringUtil.isNotEmpty(spuDTO.getTitle())){
            criteria.andLike("title","%" + spuDTO.getTitle() + "%");
        }
        if(ObjectUtil.isNotNull(spuDTO.getSaleable()) && spuDTO.getSaleable() != 2){
            criteria.andEqualTo("saleable",spuDTO.getSaleable());
        }

        //排序
        if(ObjectUtil.isNotNull(spuDTO.getSort())){
            example.setOrderByClause(spuDTO.getOrderByClause());
        }

        List<SpuEntity> list = spuMapper.selectByExample(example);

        //商品分类

        //优化
        List<SpuDTO> spuDtoList = list.stream().map(spuEntity -> {

            SpuDTO spuDTO1 = BaiduBeanUtil.copyProperties(spuEntity, SpuDTO.class);

            //设置品牌名称
            BrandDTO brandDTO = new BrandDTO();
            brandDTO.setId(spuEntity.getBrandId());
            Result<PageInfo<BrandEntity>> brandInfo = brandService.getBrandInfo(brandDTO);
            if (ObjectUtil.isNotNull(brandInfo)) {

                PageInfo<BrandEntity> data = brandInfo.getData();
                List<BrandEntity> list1 = data.getList();

                if (!list1.isEmpty() && list1.size() == 1) {
                    spuDTO1.setBrandName(list1.get(0).getName());
                }
            }

            //设置分类
            //分类名称
            List<CategoryEntity> categoryEntityList = categoryMapper.selectByIdList(Arrays.asList(spuDTO1.getCid1(), spuDTO1.getCid2(), spuDTO1.getCid3()));
            String categoryName = categoryEntityList.stream().map(category -> {
                return category.getName();
            }).collect(Collectors.joining("/"));
            spuDTO1.setCategoryName(categoryName);

            return spuDTO1;
        }).collect(Collectors.toList());


//        List<SpuDTO> spuDTOS = new ArrayList<>();
//        list.stream().forEach(spuEntity -> {
//
//            //通过品牌id查询品牌名称
//
//            SpuDTO spuDTO1 = BaiduBeanUtil.copyProperties(spuEntity, SpuDTO.class);
//
//            BrandDTO brandDTO = new BrandDTO();
//            brandDTO.setId(spuEntity.getBrandId());
//            Result<PageInfo<BrandEntity>> brandInfo = brandService.getBrandInfo(brandDTO);
//            if (ObjectUtil.isNotNull(brandInfo)) {
//
//                PageInfo<BrandEntity> data = brandInfo.getData();
//                List<BrandEntity> list1 = data.getList();
//
//                if(!list1.isEmpty() && list1.size() == 1){
//                    spuDTO1.setBrandName(list1.get(0).getName());
//                }
//            }
//
//        });

        PageInfo<SpuEntity> info = new PageInfo<>(list);

//        Map<String, Object> map = new HashMap<>();
//        map.put("list",spuDtoList);
//        map.put("total",info.getTotal());
//
//        return this.setResultSuccess(map);
        return this.setResult(HTTPStatus.OK,info.getTotal()+ "",spuDtoList);
    }
}
