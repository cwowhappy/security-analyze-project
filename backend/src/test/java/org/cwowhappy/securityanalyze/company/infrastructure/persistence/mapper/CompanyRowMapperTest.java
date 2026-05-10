package org.cwowhappy.securityanalyze.company.infrastructure.persistence.mapper;

import org.cwowhappy.securityanalyze.company.infrastructure.persistence.entity.CompanyEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CompanyRowMapper 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class CompanyRowMapperTest {

    private final CompanyRowMapper mapper = new CompanyRowMapper();

    @Test
    void shouldMapResultSetToCompanyEntity() throws Exception {
        // 准备 Mock 数据
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0, 0);

        when(rs.getString("id")).thenReturn("comp456");
        when(rs.getString("unified_social_credit_code")).thenReturn("91440300192336893H");
        when(rs.getString("name")).thenReturn("平安银行股份有限公司");
        when(rs.getString("short_name")).thenReturn("平安银行");
        when(rs.getString("english_name")).thenReturn("Ping An Bank Co., Ltd.");
        when(rs.getString("former_name")).thenReturn("深圳发展银行股份有限公司");
        when(rs.getString("legal_representative")).thenReturn("谢永林");
        when(rs.getString("chairman")).thenReturn("谢永林");
        when(rs.getString("manager")).thenReturn("胡跃飞");
        when(rs.getString("secretary")).thenReturn("周强");
        when(rs.getBigDecimal("reg_capital")).thenReturn(new BigDecimal("19405918198.00"));
        when(rs.getDate("setup_date")).thenReturn(Date.valueOf("1987-12-22"));
        when(rs.getString("province")).thenReturn("广东省");
        when(rs.getString("city")).thenReturn("深圳市");
        when(rs.getString("reg_address")).thenReturn("深圳市罗湖区深南东路5047号");
        when(rs.getString("office_address")).thenReturn("深圳市福田区益田路5033号平安金融中心");
        when(rs.getString("website")).thenReturn("http://bank.pingan.com");
        when(rs.getString("industry")).thenReturn("货币金融服务");
        when(rs.getString("main_business")).thenReturn("吸收公众存款；发放短期、中期和长期贷款等");
        when(rs.getString("business_scope")).thenReturn("办理人民币存、贷、结算、汇兑业务等");
        when(rs.getString("introduction")).thenReturn("平安银行是一家总部设在深圳的全国性股份制商业银行。");
        when(rs.getObject("employees", Integer.class)).thenReturn(44277);
        when(rs.getString("controller_name")).thenReturn("中国平安保险（集团）股份有限公司");
        when(rs.getString("controller_type")).thenReturn("法人");
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

        // 执行映射
        CompanyEntity entity = mapper.mapRow(rs, 1);

        // 验证所有字段
        assertThat(entity.getId()).isEqualTo("comp456");
        assertThat(entity.getUnifiedSocialCreditCode()).isEqualTo("91440300192336893H");
        assertThat(entity.getName()).isEqualTo("平安银行股份有限公司");
        assertThat(entity.getShortName()).isEqualTo("平安银行");
        assertThat(entity.getEnglishName()).isEqualTo("Ping An Bank Co., Ltd.");
        assertThat(entity.getFormerName()).isEqualTo("深圳发展银行股份有限公司");
        assertThat(entity.getLegalRepresentative()).isEqualTo("谢永林");
        assertThat(entity.getChairman()).isEqualTo("谢永林");
        assertThat(entity.getManager()).isEqualTo("胡跃飞");
        assertThat(entity.getSecretary()).isEqualTo("周强");
        assertThat(entity.getRegCapital()).isEqualTo(new BigDecimal("19405918198.00"));
        assertThat(entity.getSetupDate()).isEqualTo(LocalDate.of(1987, 12, 22));
        assertThat(entity.getProvince()).isEqualTo("广东省");
        assertThat(entity.getCity()).isEqualTo("深圳市");
        assertThat(entity.getRegAddress()).isEqualTo("深圳市罗湖区深南东路5047号");
        assertThat(entity.getOfficeAddress()).isEqualTo("深圳市福田区益田路5033号平安金融中心");
        assertThat(entity.getWebsite()).isEqualTo("http://bank.pingan.com");
        assertThat(entity.getIndustry()).isEqualTo("货币金融服务");
        assertThat(entity.getMainBusiness()).isEqualTo("吸收公众存款；发放短期、中期和长期贷款等");
        assertThat(entity.getBusinessScope()).isEqualTo("办理人民币存、贷、结算、汇兑业务等");
        assertThat(entity.getIntroduction()).isEqualTo("平安银行是一家总部设在深圳的全国性股份制商业银行。");
        assertThat(entity.getEmployees()).isEqualTo(44277);
        assertThat(entity.getControllerName()).isEqualTo("中国平安保险（集团）股份有限公司");
        assertThat(entity.getControllerType()).isEqualTo("法人");
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }
}
