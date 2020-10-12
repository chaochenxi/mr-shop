package com.baidu.shop.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baidu.shop.base.BaseApiService;
import com.baidu.shop.base.Result;
import com.baidu.shop.document.GoodsDocument;
import com.baidu.shop.dto.SkuDTO;
import com.baidu.shop.dto.SpecParamDTO;
import com.baidu.shop.dto.SpuDTO;
import com.baidu.shop.entity.BrandEntity;
import com.baidu.shop.entity.CategoryEntity;
import com.baidu.shop.entity.SpecParamEntity;
import com.baidu.shop.entity.SpuDetailEntity;
import com.baidu.shop.feign.BrandFeign;
import com.baidu.shop.feign.CategoryFeign;
import com.baidu.shop.feign.GoodsFeign;
import com.baidu.shop.feign.SpecificationFeign;
import com.baidu.shop.response.GoodsResponse;
import com.baidu.shop.service.ShopElasticsearchService;
import com.baidu.shop.status.HTTPStatus;
import com.baidu.shop.utils.ESHighLightUtil;
import com.baidu.shop.utils.JSONUtil;

import com.baidu.shop.utils.StringUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName ShopElasticsearchServiceImpl
 * @Description: TODO
 * @Author chaochenxi
 * @Date 2020/9/16
 * @Version V1.0
 **/
@RestController
@Slf4j
public class ShopElasticsearchServiceImpl extends BaseApiService implements ShopElasticsearchService {

    @Autowired
    private GoodsFeign goodsFeign;

    @Autowired
    private SpecificationFeign specificationFeign;

    @Autowired
    private CategoryFeign categoryFeign;

    @Autowired
    private BrandFeign brandFeign;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public Result<JSONObject> saveData(Integer spuId) {

        //通过spuId查询数据
        SpuDTO spuDTO = new SpuDTO();
        spuDTO.setId(spuId);
        Result<List<SpuDTO>> spuResult = goodsFeign.getSpuInfo(spuDTO);

        if(spuResult.getCode() == 200){
            if(!spuResult.getData().isEmpty()){
                elasticsearchRestTemplate.save(spuResult.getData().get(0));
            }
        }

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> delData(Integer spuId) {
        return null;
    }

    @Override
    public GoodsResponse search(String search,Integer page,String filter) {
        System.out.println(filter);

        //判断搜索内容不能为空
        if (StringUtil.isEmpty(search)) {
            throw new RuntimeException("搜索内容不能为空");
        }
        SearchHits<GoodsDocument> searchHits = elasticsearchRestTemplate.search(this.getSearchQueryBuilder(search,page,filter).build(), GoodsDocument.class);

        List<SearchHit<GoodsDocument>> searchHits1 = searchHits.getSearchHits();
        List<SearchHit<GoodsDocument>> highLightHits = ESHighLightUtil.getHighLightHit(searchHits1);

        //要返回的内容
        List<GoodsDocument> goodsList = highLightHits.stream().map(searchHit -> searchHit.getContent()).collect(Collectors.toList());

        //总条数&总页数
        long total = searchHits.getTotalHits();
        long totalPage = Double.valueOf(Math.ceil(Long.valueOf(total).doubleValue() / 10)).longValue();

        Aggregations aggregations = searchHits.getAggregations();

        //List<CategoryEntity> categoryList = this.getCategoryList(aggregations);//获取分类集合

        Map<Integer, List<CategoryEntity>> map = this.getCategoryList(aggregations);

        List<CategoryEntity> categoryList = null;

        Integer hotCid = 0;

        for (Map.Entry<Integer,List<CategoryEntity>> mapEntry : map.entrySet()){
            hotCid = mapEntry.getKey();
            categoryList = mapEntry.getValue();
        }

        //通过cid去查询规格参数
        Map<String, List<String>> specParamValueMap = this.getSpecParam(hotCid,search);

        List<BrandEntity> brandList = this.getBrandList(aggregations);//获取品牌集合

        return new GoodsResponse(total, totalPage, brandList, categoryList, goodsList, specParamValueMap);
    }

    private Map<String, List<String>> getSpecParam(Integer hotCid,String search){
        SpecParamDTO specParamDTO = new SpecParamDTO();
        specParamDTO.setCid(hotCid);
        specParamDTO.setSearching(true);//只搜索有查询属性的规格参数
        Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
        if(specParamResult.getCode() == 200){
            List<SpecParamEntity> specParamList = specParamResult.getData();
            //聚合查询
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"brandName","categoryName","title"));
            //分页必须得查询一条数据
            queryBuilder.withPageable(PageRequest.of(0,1));

            specParamList.stream().forEach(specParam -> {
                queryBuilder.addAggregation(AggregationBuilders.terms(specParam.getName()).field("specs." + specParam.getName() + ".keyword"));
            });

            SearchHits<GoodsDocument> searchHits = elasticsearchRestTemplate.search(queryBuilder.build(), GoodsDocument.class);

            Map<String, List<String>> map = new HashMap<>();
            Aggregations aggregations = searchHits.getAggregations();
            specParamList.stream().forEach(specParam -> {

                Terms terms = aggregations.get(specParam.getName());
                List<? extends Terms.Bucket> buckets = terms.getBuckets();
                List<String> valueList = buckets.stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());

                map.put(specParam.getName(),valueList);
            });
            return map;
        }
        return null;
    }

    private NativeSearchQueryBuilder getSearchQueryBuilder(String search, Integer page,String filter){

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        if(StringUtil.isNotEmpty(filter) && filter.length() > 2){
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            Map<String, String> filterMap = JSONUtil.toMapValueString(filter);

            filterMap.forEach((key,value) -> {
                MatchQueryBuilder matchQueryBuilder = null;

                //分类 品牌和 规格参数的查询方式不一样
                if(key.equals("cid3") || key.equals("brandId")){
                    matchQueryBuilder = QueryBuilders.matchQuery(key, value);
                }else{
                    matchQueryBuilder = QueryBuilders.matchQuery("specs." + key + ".keyword",value);
                }
                boolQueryBuilder.must(matchQueryBuilder);
            });
            nativeSearchQueryBuilder.withFilter(boolQueryBuilder);
        }

        //match通过值只能查询一个字段,multiMatch通过值可以查询多个字段
        nativeSearchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(search,"title","brandName","categoryName"));

        //品牌
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("cid_agg").field("cid3"));
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("brand_agg").field("brandId"));

        //分页
        nativeSearchQueryBuilder.withPageable(PageRequest.of(page-1,10));

        //高亮
        nativeSearchQueryBuilder.withHighlightBuilder(ESHighLightUtil.getHighlightBuilder("title"));

        return nativeSearchQueryBuilder;
    }

    private Map<Integer,List<CategoryEntity>> getCategoryList(Aggregations aggregations){
        Terms cid_agg = aggregations.get("cid_agg");
        List<? extends Terms.Bucket> cidBuckets = cid_agg.getBuckets();

        List<Integer> hotCidArr = Arrays.asList(0);

        List<Long> maxCount = Arrays.asList(0L);

        Map<Integer,List<CategoryEntity>> map = new HashMap<>();

        List<String> cidList = cidBuckets.stream().map(cidBucket -> {
            Number keyAsNumber = cidBucket.getKeyAsNumber();

            if(cidBucket.getDocCount() > maxCount.get(0)){
                maxCount.set(0,cidBucket.getDocCount());
                hotCidArr.set(0,keyAsNumber.intValue());
            }

            return keyAsNumber.intValue() + "";
        }).collect(Collectors.toList());

        String cidsStr = String.join(",", cidList);
        Result<List<CategoryEntity>> categoryResult = categoryFeign.getCategoryByIdList(cidsStr);

        map.put(hotCidArr.get(0),categoryResult.getData());

        return map;

    }

    private List<BrandEntity> getBrandList(Aggregations aggregations){

        Terms brand_agg = aggregations.get("brand_agg");
        List<String> brandIdList = brand_agg.getBuckets().stream().map(brandBucket -> brandBucket.getKeyAsNumber().intValue() + "").collect(Collectors.toList());
        Result<List<BrandEntity>> brandResult = brandFeign.getBrandByIdList(String.join(",", brandIdList));
        return brandResult.getData();
    }

    @Override
    public Result<JSONObject> initEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDocument.class);
        if(!indexOperations.exists()){
            indexOperations.create();
            log.info("索引创建成功");
            indexOperations.createMapping();
            log.info("映射创建成功");
        }

        //批量新增数据
        List<GoodsDocument> goodsDocs = this.esGoodsInfo();
        elasticsearchRestTemplate.save(goodsDocs);

        return this.setResultSuccess();
    }

    @Override
    public Result<JSONObject> clearEsData() {
        IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(GoodsDocument.class);
        if(indexOperations.exists()){
            indexOperations.delete();
            log.info("索引删除成功");
        }

        return this.setResultSuccess();
    }

    private List<GoodsDocument> esGoodsInfo() {

        //查询出来的数据是多个spu
        List<GoodsDocument> goodsDocuments = new ArrayList<>();

        //查询spu信息
        SpuDTO spuDTO = new SpuDTO();
        Result<List<SpuDTO>> spuInfo = goodsFeign.getSpuInfo(spuDTO);
        if(spuInfo.getCode() == HTTPStatus.OK){

            //spu数据
            List<SpuDTO> spuList = spuInfo.getData();

            spuList.stream().forEach(spu -> {

                GoodsDocument goodsDocument = new GoodsDocument();

                goodsDocument.setId(spu.getId().longValue());
                goodsDocument.setTitle(spu.getTitle());
                goodsDocument.setSubTitle(spu.getSubTitle());
                goodsDocument.setBrandName(spu.getBrandName());
                goodsDocument.setCategoryName(spu.getCategoryName());
                goodsDocument.setBrandId(spu.getBrandId().longValue());
                goodsDocument.setCid1(spu.getCid1().longValue());
                goodsDocument.setCid2(spu.getCid2().longValue());
                goodsDocument.setCid3(spu.getCid3().longValue());
                goodsDocument.setCreateTime(spu.getCreateTime());

                //通过spuID查询skuList
                Result<List<SkuDTO>> skuResult = goodsFeign.getSkuBySpuId(spu.getId());

                List<Long> priceList = new ArrayList<>();
                List<Map<String, Object>> skuMap = null;

                if(skuResult.getCode() == HTTPStatus.OK){

                    List<SkuDTO> skuList = skuResult.getData();

                    skuMap = skuList.stream().map(sku -> {
                        Map<String, Object> map = new HashMap<>();

                        map.put("id", sku.getId());
                        map.put("title", sku.getTitle());
                        map.put("images", sku.getImages());
                        map.put("price", sku.getPrice());

                        priceList.add(sku.getPrice().longValue());

                        return map;

                    }).collect(Collectors.toList());

                }

                //查询规格参数
                SpecParamDTO specParamDTO = new SpecParamDTO();
                specParamDTO.setCid(spuDTO.getCid3());
                Result<List<SpecParamEntity>> specParamResult = specificationFeign.getSpecParamInfo(specParamDTO);
                Map<String, Object> specMap = new HashMap<>();

                if (specParamResult.getCode() == HTTPStatus.OK) {
                    //只有规格参数的id和规格参数的名字
                    List<SpecParamEntity> paramList = specParamResult.getData();

                    //通过spuid查询spuDetail,detail里面有通用和特殊规格参数的值
                    Result<SpuDetailEntity> spuDetailResult = goodsFeign.getSpuDetailBydSpu(spu.getId());

                    if(spuDetailResult.getCode() == HTTPStatus.OK){

                        SpuDetailEntity spuDetaiInfo = spuDetailResult.getData();

                        //通用规格参数的值
                        String genericSpecStr = spuDetaiInfo.getGenericSpec();
                        Map<String, String> genericSpecMap = JSONUtil.toMapValueString(genericSpecStr);

                        //特有规格参数的值
                        String specialSpecStr = spuDetaiInfo.getSpecialSpec();
                        Map<String, List<String>> specialSpecMap = JSONUtil.toMapValueStrList(specialSpecStr);

                        paramList.stream().forEach(param -> {

                            if (param.getGeneric()) {

                                if(param.getNumeric() && param.getSearching()){
                                    specMap.put(param.getName(), this.chooseSegment(genericSpecMap.get(param.getId() + ""),param.getSegments(),param.getUnit()));
                                }else{
                                    specMap.put(param.getName(), genericSpecMap.get(param.getId() + ""));
                                }
                            } else {
                                specMap.put(param.getName(), specialSpecMap.get(param.getId() + ""));
                            }

                        });

                    }

                }

                goodsDocument.setSpecs(specMap);
                goodsDocument.setPrice(priceList);
                goodsDocument.setSkus(JSONUtil.toJsonString(skuMap));
                goodsDocuments.add(goodsDocument);

            });
        }
        return goodsDocuments;
    }

    //把具体的值转换成区间-->不做范围查询
    private String chooseSegment(String value, String segments, String unit) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : segments.split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + unit + "以上";
                }else if(begin == 0){
                    result = segs[1] + unit + "以下";
                }else{
                    result = segment + unit;
                }
                break;
            }
        }
        return result;
    }


}
