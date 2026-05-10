package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cwowhappy.securityanalyze.company.application.dto.CompanyDTO;
import org.cwowhappy.securityanalyze.company.application.service.CompanyAppService;
import org.cwowhappy.securityanalyze.interfaces.rest.request.CreateCompanyRequest;
import org.cwowhappy.securityanalyze.shared.dto.PageQuery;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CompanyController Web 层测试（@WebMvcTest，只加载 Controller 层）。
 */
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyAppService companyAppService;

    @Test
    void shouldReturnCompaniesWhenListCompanies() throws Exception {
        // Arrange
        CompanyDTO dto = CompanyDTO.builder()
                .id("comp001")
                .unifiedSocialCreditCode("9144030019218538XX")
                .name("平安银行股份有限公司")
                .shortName("平安银行")
                .industry("银行")
                .province("广东省")
                .build();
        PageResult<CompanyDTO> pageResult = PageResult.<CompanyDTO>builder()
                .list(List.of(dto))
                .total(1)
                .page(1)
                .size(20)
                .build();
        when(companyAppService.findByPage(any(PageQuery.class), eq("银行"), eq("广东省"), eq("平安")))
                .thenReturn(pageResult);

        // Act & Assert
        mockMvc.perform(get("/api/companies")
                        .param("industry", "银行")
                        .param("province", "广东省")
                        .param("keyword", "平安"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.list[0].name").value("平安银行股份有限公司"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldReturnCompanyWhenFoundByUscCode() throws Exception {
        // Arrange
        CompanyDTO dto = CompanyDTO.builder()
                .id("comp001")
                .unifiedSocialCreditCode("9144030019218538XX")
                .name("平安银行股份有限公司")
                .shortName("平安银行")
                .industry("银行")
                .province("广东省")
                .build();
        when(companyAppService.findByUscCode("9144030019218538XX")).thenReturn(Optional.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/companies/9144030019218538XX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unifiedSocialCreditCode").value("9144030019218538XX"))
                .andExpect(jsonPath("$.data.name").value("平安银行股份有限公司"));
    }

    @Test
    void shouldCreateCompanyWhenRequestValid() throws Exception {
        // Arrange
        CreateCompanyRequest request = new CreateCompanyRequest();
        request.setUnifiedSocialCreditCode("9144030019218538XX");
        request.setName("平安银行股份有限公司");
        request.setShortName("平安银行");
        request.setIndustry("银行");
        request.setProvince("广东省");
        request.setCity("深圳市");
        request.setRegCapital(new BigDecimal("1940591.8198"));
        request.setSetupDate(LocalDate.of(1987, 12, 22));
        request.setEmployees(44277);

        when(companyAppService.createCompany(any(CompanyDTO.class))).thenReturn("comp001");

        // Act & Assert
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("comp001"));
    }

    @Test
    void shouldReturnBadRequestWhenNameBlank() throws Exception {
        // Arrange
        CreateCompanyRequest request = new CreateCompanyRequest();
        request.setUnifiedSocialCreditCode("9144030019218538XX");
        request.setName("");
        request.setIndustry("银行");

        // Act & Assert
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
