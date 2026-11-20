package com.airticket.mapper;

import com.airticket.entity.Tdjp;
import com.airticket.vo.TdjpVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TdjpMapper {

    @Select("SELECT * FROM tdjp WHERE ddid = #{ddid}")
    Tdjp findByDdid(Integer ddid);

    @Insert("INSERT INTO tdjp (ddid, canceltime, process) VALUES (#{ddid}, #{canceltime}, #{process})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tdjp tdjp);

    List<TdjpVO> findAll(Map<String, Object> params);

    Long countAll(Map<String, Object> params);

    @Select("SELECT * FROM tdjp WHERE id = #{id}")
    Tdjp findById(Integer id);

    @Update("UPDATE tdjp SET process = #{process} WHERE id = #{id}")
    int updateProcess(@Param("id") Integer id, @Param("process") Integer process);

    @Select("SELECT * FROM tdjp WHERE process = #{process}")
    List<Tdjp> findByProcess(Integer process);
}
