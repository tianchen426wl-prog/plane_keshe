package com.airticket.mapper;

import com.airticket.entity.Company;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CompanyMapper {

    @Select("SELECT * FROM company")
    List<Company> findAll();

    @Select("SELECT * FROM company WHERE company_name LIKE CONCAT('%', #{name}, '%')")
    List<Company> findByName(String name);

    @Select("SELECT * FROM company WHERE company_id = #{companyId}")
    Company findById(Integer companyId);

    @Insert("INSERT INTO company (company_name, company_profile, company_picture, company_culture, company_phone, company_purport) " +
            "VALUES (#{companyName}, #{companyProfile}, #{companyPicture}, #{companyCulture}, #{companyPhone}, #{companyPurport})")
    @Options(useGeneratedKeys = true, keyProperty = "companyId")
    int insert(Company company);

    @Update("UPDATE company SET company_name = #{companyName}, company_profile = #{companyProfile}, " +
            "company_picture = #{companyPicture}, company_culture = #{companyCulture}, " +
            "company_phone = #{companyPhone}, company_purport = #{companyPurport} WHERE company_id = #{companyId}")
    int update(Company company);

    @Delete("DELETE FROM company WHERE company_id = #{companyId}")
    int deleteById(Integer companyId);
}