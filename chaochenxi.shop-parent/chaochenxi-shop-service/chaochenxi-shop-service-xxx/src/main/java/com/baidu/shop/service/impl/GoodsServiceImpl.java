package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.dto.BrandDTO;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.*;
import com.baidu.shop.mapper.*;
import com.baidu.shop.service.BrandService;
import com.baidu.shop.service.GoodsService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.BaiduBeanUtil;
import com.baidu.shop.utils.ObjectUtil;
import com.baidu.shop.utils.StringUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import com.netflix.discovery.converters.Auto;
import com.netflix.ribbon.proxy.annotation.Http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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

    @Resource
    private SpuDetailMapper spuDetailMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private StockMapper stockMapper;

    @Override
    public Result<JSONObject> delGoods(Integer spuId) {

        //删除spu
        spuMapper.deleteByPrimaryKey(spuId);
        //删除spuDetail
        spuDetailMapper.deleteByPrimaryKey(spuId);

        //查询
        List<Long> skuIdArr = this.getSkuIdArrBySpuId(spuId);
        if(skuIdArr.size() > 0){//尽量加上判断避免全表数据被删除!!!!!!!!!!!!!!!
            //删除skus
            skuMapper.deleteByIdList(skuIdArr);
            //删除stock,与修改时的逻辑一样,先查询出所有将要修改skuId然后批量删除
            stockMapper.deleteByIdList(skuIdArr);
        }

        return this.setResultSuccess();
    }

    //重复代码抽取出来
    private List<Long> getSkuIdArrBySpuId(Integer spuId){
        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuId);
        List<SkuEntity> skuEntities = skuMapper.selectByExample(example);
        return skuEntities.stream().map(sku -> sku.getId()).collect(Collectors.toList());
    }

    @Override
    public Result<SpuDetailEntity> getSpuDetailBydSpu(Integer spuId) {

        SpuDetailEntity spuDetailEntity = spuDetailMapper.selectByPrimaryKey(spuId);
        return this.setResultSuccess(spuDetailEntity);
    }

    @Override
    public Result<List<SkuDTO>> getSkuBySpuId(Integer spuId) {

        List<SkuDTO> list = skuMapper.selectSkuAndStockBySpuId(spuId);
        return this.setResultSuccess(list);
    }

    @Transactional
    @Override
    public Result<JSONObject> editInfo(SpuDTO spuDTO) {
        Date date = new Date();
        //修改spu信息
        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setLastUpdateTime(date);//!!!!!!!!!设置最后更新时间

        spuMapper.updateByPrimaryKeySelective(spuEntity);
        //修改spudetail
        spuDetailMapper.updateByPrimaryKeySelective(BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(),SpuDetailEntity.class));

        Example example = new Example(SkuEntity.class);
        example.createCriteria().andEqualTo("spuId",spuDTO.getId());
        //通过spuId查询出来将要被删除的Sku
        List<SkuEntity> skuEntityList = skuMapper.selectByExample(example);
        List<Long> skuIdList = skuEntityList.stream().map(sku -> {
            return sku.getId();
        }).collect(Collectors.toList());
        //通过skuId集合删除sku
        skuMapper.deleteByIdList(skuIdList);
        //通过skuId集合删除stock
        stockMapper.deleteByIdList(skuIdList);

        //新增 sku和stock数据
        spuDTO.getSkus().stream().forEach(skuDTO -> {
            //新增sku
            SkuEntity skuEntity = BaiduBeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuDTO.getId());
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);

        });
        return this.setResultSuccess();
    }

    @Transactional
    @Override
    public Result<JSONObject> addGoods(SpuDTO spuDTO) {

        System.out.println(spuDTO);

        Date date = new Date();

        SpuEntity spuEntity = BaiduBeanUtil.copyProperties(spuDTO, SpuEntity.class);
        spuEntity.setSaleable(1);
        spuEntity.setValid(1);
        spuEntity.setCreateTime(date);
        spuEntity.setLastUpdateTime(date);
        //新增spu
        spuMapper.insertSelective(spuEntity);

        Integer spuId = spuEntity.getId();

        //新增spudetail
        SpuDetailEntity spuDetailEntity = BaiduBeanUtil.copyProperties(spuDTO.getSpuDetail(), SpuDetailEntity.class);
        spuDetailEntity.setSpuId(spuId);
        spuDetailMapper.insertSelective(spuDetailEntity);

        spuDTO.getSkus().stream().forEach(skuDTO -> {
            //新增sku
            SkuEntity skuEntity = BaiduBeanUtil.copyProperties(skuDTO, SkuEntity.class);
            skuEntity.setSpuId(spuId);
            skuEntity.setCreateTime(date);
            skuEntity.setLastUpdateTime(date);
            skuMapper.insertSelective(skuEntity);

            //新增stock
            StockEntity stockEntity = new StockEntity();
            stockEntity.setSkuId(skuEntity.getId());
            stockEntity.setStock(skuDTO.getStock());
            stockMapper.insertSelective(stockEntity);

        });

        return this.setResultSuccess();
    }

    @Override
    public Result<List<SpuDTO>> getSpuInfo(SpuDTO spuDTO) {

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
