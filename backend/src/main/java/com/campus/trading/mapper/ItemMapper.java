package com.campus.trading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.trading.entity.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ItemMapper extends BaseMapper<Item> {

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM item")
    Long selectTotalViews();

    @Select("SELECT COUNT(*) FROM item WHERE status = #{status}")
    Long selectCountByStatus(@Param("status") int status);
}
