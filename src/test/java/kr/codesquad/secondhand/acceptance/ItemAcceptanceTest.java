package kr.codesquad.secondhand.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.IOException;
import java.util.List;
import kr.codesquad.secondhand.SupportRepository;
import kr.codesquad.secondhand.application.image.S3Uploader;
import kr.codesquad.secondhand.domain.member.Member;
import kr.codesquad.secondhand.fixture.FixtureFactory;
import kr.codesquad.secondhand.infrastructure.jwt.JwtProvider;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
public class ItemAcceptanceTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private SupportRepository supportRepository;

    @MockBean
    private S3Uploader s3Uploader;

    private File createFakeFile() throws IOException {
        return File.createTempFile("test-image", ".png");
    }

    @DisplayName("상품 등록할 때")
    @Nested
    class Register {

        @DisplayName("상품 이미지와 상품 등록정보가 주어지면 상품 등록에 성공한다.")
        @Test
        void givenImagesAndItemData_whenRegisterItem_thenSuccess() throws Exception {
            // given
            givenSetUp();

            var request = RestAssured
                    .given().log().all()
                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L))
                    .multiPart("images",
                            createFakeFile(),
                            MediaType.IMAGE_PNG_VALUE)
                    .multiPart("images",
                            createFakeFile(),
                            MediaType.IMAGE_PNG_VALUE)
                    .multiPart("item",
                            objectMapper.writeValueAsString(FixtureFactory.createItemRegisterRequest()),
                            MediaType.APPLICATION_JSON_VALUE);

            // when
            var response = registerItem(request);

            // then
            assertAll(
                    () -> assertThat(response.statusCode()).isEqualTo(201),
                    () -> assertThat(response.jsonPath().getInt("statusCode")).isEqualTo(201)
            );
        }

        @DisplayName("상품 이미지가 아예 주어지지 않으면 400 응답코드로 응답한다.")
        @Test
        void givenNoImageAndItemData_whenRegisterItem_thenResponse400() throws Exception {
            // given
            givenSetUp();

            // when
            var request = RestAssured
                    .given().log().all()
                    .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtProvider.createAccessToken(1L))
                    .multiPart("item",
                            objectMapper.writeValueAsString(FixtureFactory.createItemRegisterRequest()),
                            MediaType.APPLICATION_JSON_VALUE);

            // when
            var response = registerItem(request);

            // then
            assertAll(
                    () -> assertThat(response.statusCode()).isEqualTo(400),
                    () -> assertThat(response.jsonPath().getInt("statusCode")).isEqualTo(400),
                    () -> assertThat(response.jsonPath().getString("message")).isNotNull()
            );
        }

        private void givenSetUp() {
            // objectMapper 한글 인코딩을 위한 설정
            objectMapper.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);

            supportRepository.save(Member.builder()
                    .email("23Yong@secondhand.com")
                    .loginId("23Yong")
                    .profileUrl("image-url")
                    .build());
            given(s3Uploader.uploadImageFiles(anyList())).willReturn(List.of("url1", "url2"));
        }

        private ExtractableResponse<Response> registerItem(RequestSpecification request) {
            return request
                    .when()
                    .post("/api/items")
                    .then().log().all()
                    .extract();
        }
    }
}