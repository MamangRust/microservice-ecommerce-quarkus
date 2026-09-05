package com.sanedge.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private RoleQueryServiceImpl roleQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        roleQueryService = new RoleQueryServiceImpl(
                roleRepository,
                redisService,
                tracingMetrics,
                objectMapper);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Role createMockRole(Long id, String roleName) {
        Role role = new Role();
        role.id = id != null ? id.longValue() : null;
        role.setRoleName(roleName);
        role.setDeletedAt(null);
        role.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        role.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return role;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize in test helper", e);
        }
    }

    @Test
    void findById_cacheHit_returnsCachedRoleWithoutHittingDb() {
        Role role = createMockRole(1L, "AdminRole");
        RoleResponse roleResponse = RoleResponse.from(role);
        String cachedJson = toJson(roleResponse);

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findById(1L);
        ApiResponse<RoleResponse> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("AdminRole");

        verify(roleRepository, never()).findById(anyLong());
    }

    @Test
    void findById_cacheMiss_fetchesFromDbAndSavesToCache() {
        Role role = createMockRole(2L, "EditorRole");

        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(role));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findById(2L);
        ApiResponse<RoleResponse> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("EditorRole");

        verify(roleRepository).findById(anyLong());

        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findById_cacheMiss_roleNotFoundInDb_throwsNotFoundException() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findById(999L);

        assertThrows(NotFoundException.class, () -> resultUni.await().indefinitely());

        verify(redisService, never()).setReactive(anyString(), anyString());
    }

    @Test
    void findByName_cacheHit_returnsCachedRoleWithoutHittingDb() {
        Role role = createMockRole(1L, "Admin");
        RoleResponse roleResponse = RoleResponse.from(role);
        String cachedJson = toJson(roleResponse);

        when(redisService.getReactive("role:name:Admin")).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findByName("Admin");
        ApiResponse<RoleResponse> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("Admin");

        verify(roleRepository, never()).findByRoleName(anyString());
    }

    @Test
    void findByName_cacheMiss_fetchesFromDbAndSavesToCache() {
        Role role = createMockRole(2L, "Editor");

        when(redisService.getReactive("role:name:Editor")).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().item(role));
        when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findByName("Editor");
        ApiResponse<RoleResponse> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role found");
        assertThat(response.data().getName()).isEqualTo("Editor");

        verify(roleRepository).findByRoleName("Editor");
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findByName_cacheMiss_roleNotFoundInDb_throwsNotFoundException() {
        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().nullItem());

        Uni<ApiResponse<RoleResponse>> resultUni = roleQueryService.findByName("NonExistent");

        assertThrows(NotFoundException.class, () -> resultUni.await().indefinitely());

        verify(redisService, never()).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_cacheHit_returnsCachedRolesWithoutHittingDb() {
        Long userId = 1L;

        RoleResponse res1 = RoleResponse.from(createMockRole(1L, "Admin"));
        RoleResponse res2 = RoleResponse.from(createMockRole(2L, "Editor"));

        List<RoleResponse> cachedList = List.of(res1, res2);
        String cachedJson = toJson(cachedList);

        when(redisService.getReactive("roles:user:1")).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponse<List<RoleResponse>>> resultUni = roleQueryService.findByUserId(userId);
        ApiResponse<List<RoleResponse>> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles found");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Admin");

        verify(roleRepository, never()).findUserRoles(anyLong());
    }

    @Test
    void findByUserId_cacheMiss_fetchesFromDbAndCachesResult() {
        Long userId = 2L;

        Role role1 = createMockRole(1L, "Role1");
        Role role2 = createMockRole(2L, "Role2");

        List<Role> roles = List.of(role1, role2);

        when(redisService.getReactive("roles:user:2")).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findUserRoles(userId))
                .thenReturn(Uni.createFrom().item(roles));
        when(redisService.setReactive(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponse<List<RoleResponse>>> resultUni = roleQueryService.findByUserId(userId);
        ApiResponse<List<RoleResponse>> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles found");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Role1");

        verify(roleRepository).findUserRoles(userId);
        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findByUserId_emptyRoleList_returnsEmptyArray() {
        Long userId = 999L;
        String cacheKey = "roles:user:999";

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findUserRoles(userId))
                .thenReturn(Uni.createFrom().item(List.of()));
        when(redisService.setReactive(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponse<List<RoleResponse>>> resultUni = roleQueryService.findByUserId(userId);
        ApiResponse<List<RoleResponse>> response = resultUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles found");
        assertThat(response.data()).isEmpty();

        verify(roleRepository).findUserRoles(userId);

        verify(redisService).setReactive(anyString(), anyString());
    }

    @Test
    void findAllPaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:all:1:10:null";

        RoleResponse res1 = RoleResponse.from(createMockRole(1L, "Role1"));
        RoleResponse res2 = RoleResponse.from(createMockRole(2L, "Role2"));

        ApiResponsePagination<List<RoleResponse>> mockResponse = new ApiResponsePagination<>(
                "success", "Roles retrieved successfully", List.of(res1, res2), null);

        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponsePagination<List<RoleResponse>>> responseUni = roleQueryService.findAllPaginated(req);
        ApiResponsePagination<List<RoleResponse>> response = responseUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles retrieved successfully");
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).getName()).isEqualTo("Role1");

        verify(roleRepository, never()).findRoles(any(FindAllRoles.class));
    }

    @Test
    void findAllPaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:all:1:10:null";

        Role role1 = createMockRole(1L, "Role1");
        Role role2 = createMockRole(2L, "Role2");

        PagedResult<Role> pagedResult = new PagedResult<>(List.of(role1, role2), 2);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponsePagination<List<RoleResponse>>> responseUni = roleQueryService.findAllPaginated(req);
        ApiResponsePagination<List<RoleResponse>> response = responseUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Roles retrieved successfully");
        assertThat(response.data()).hasSize(2);
        assertThat(response.pagination()).isNotNull();
        assertThat(response.pagination().totalRecords()).isEqualTo(2);
        assertThat(response.pagination().totalPages()).isEqualTo(1);

        verify(roleRepository).findRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findActivePaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:active:1:10:null";

        RoleResponseDeleteAt res1 = RoleResponseDeleteAt.from(createMockRole(1L, "ActiveRole"));

        ApiResponsePagination<List<RoleResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Active roles retrieved successfully", List.of(res1), null);

        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> responseUni = roleQueryService.findActivePaginated(req);
        ApiResponsePagination<List<RoleResponseDeleteAt>> response = responseUni.await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(roleRepository, never()).findActiveRoles(any(FindAllRoles.class));
    }

    @Test
    void findActivePaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:active:1:10:null";

        Role activeRole = createMockRole(1L, "ActiveRole");

        PagedResult<Role> pagedResult = new PagedResult<>(List.of(activeRole), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findActiveRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> responseUni = roleQueryService.findActivePaginated(req);
        ApiResponsePagination<List<RoleResponseDeleteAt>> response = responseUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Active roles retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("ActiveRole");

        verify(roleRepository).findActiveRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findTrashedPaginated_cacheHit_returnsCachedList() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:trashed:1:10:null";

        RoleResponseDeleteAt res1 = RoleResponseDeleteAt.from(createMockRole(2L, "TrashedRole"));

        ApiResponsePagination<List<RoleResponseDeleteAt>> mockResponse = new ApiResponsePagination<>(
                "success", "Trashed roles retrieved successfully", List.of(res1), null);

        String cachedJson = toJson(mockResponse);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().item(cachedJson));

        Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> responseUni = roleQueryService.findTrashedPaginated(req);
        ApiResponsePagination<List<RoleResponseDeleteAt>> response = responseUni.await().indefinitely();

        assertThat(response.data()).hasSize(1);
        verify(roleRepository, never()).findTrashedRoles(any(FindAllRoles.class));
    }

    @Test
    void findTrashedPaginated_cacheMiss_fetchesFromDbAndCachesResult() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(10);
        req.setSearch(null);

        String cacheKey = "roles:trashed:1:10:null";

        Role trashedRole = createMockRole(2L, "TrashedRole");

        PagedResult<Role> pagedResult = new PagedResult<>(List.of(trashedRole), 1);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findTrashedRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> responseUni = roleQueryService.findTrashedPaginated(req);
        ApiResponsePagination<List<RoleResponseDeleteAt>> response = responseUni.await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Trashed roles retrieved successfully");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).getName()).isEqualTo("TrashedRole");

        verify(roleRepository).findTrashedRoles(req);
        verify(redisService).setWithExpirationReactive(anyString(), anyString(), eq(300L));
    }

    @Test
    void findAllPaginated_calculatesTotalPagesCorrectlyWhenNotPerfectlyDivisible() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(2);
        req.setSearch(null);
        String cacheKey = "roles:all:1:2:null";

        Role response1 = createMockRole(1L, "r1");
        Role response2 = createMockRole(2L, "r2");

        List<Role> roles = List.of(createMockRole(1L, "r1"), createMockRole(2L, "r2"));
        PagedResult<Role> pagedResult = new PagedResult<>(roles, 5);

        when(redisService.getReactive(cacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findRoles(req)).thenReturn(Uni.createFrom().item(pagedResult));
        when(redisService.setWithExpirationReactive(eq(cacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<ApiResponsePagination<List<RoleResponse>>> responseUni = roleQueryService.findAllPaginated(req);
        ApiResponsePagination<List<RoleResponse>> response = responseUni.await().indefinitely();

        assertThat(response.pagination()).isNotNull();
        assertThat(response.pagination().totalPages()).isEqualTo(3);
        assertThat(response.pagination().totalRecords()).isEqualTo(5);
        assertThat(response.pagination().pageSize()).isEqualTo(2);
        assertThat(response.pagination().currentPage()).isEqualTo(1);
    }

    @Test
    void findAllPaginated_withSearchKeyword_usesCorrectDynamicCacheKey() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(2);
        req.setPageSize(5);
        req.setSearch("admin role");

        String expectedCacheKey = "roles:all:2:5:admin role";

        PagedResult<Role> emptyResult = new PagedResult<>(List.of(), 0);

        when(redisService.getReactive(expectedCacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findRoles(req)).thenReturn(Uni.createFrom().item(emptyResult));
        when(redisService.setWithExpirationReactive(eq(expectedCacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        roleQueryService.findAllPaginated(req).await().indefinitely();

        verify(redisService).getReactive(expectedCacheKey);
        verify(redisService).setWithExpirationReactive(eq(expectedCacheKey), anyString(), eq(300L));
    }

    @Test
    void findActivePaginated_withSearchKeyword_usesCorrectDynamicCacheKey() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(1);
        req.setPageSize(15);
        req.setSearch("admin");

        String expectedCacheKey = "roles:active:1:15:admin";

        PagedResult<Role> emptyResult = new PagedResult<>(List.of(), 0);

        when(redisService.getReactive(expectedCacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findActiveRoles(req)).thenReturn(Uni.createFrom().item(emptyResult));
        when(redisService.setWithExpirationReactive(eq(expectedCacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        roleQueryService.findActivePaginated(req).await().indefinitely();

        verify(redisService).getReactive(expectedCacheKey);
    }

    @Test
    void findTrashedPaginated_withSearchKeyword_usesCorrectDynamicCacheKey() {
        FindAllRoles req = new FindAllRoles();
        req.setPage(3);
        req.setPageSize(20);
        req.setSearch("deleted");

        String expectedCacheKey = "roles:trashed:3:20:deleted";

        PagedResult<Role> emptyResult = new PagedResult<>(List.of(), 0);

        when(redisService.getReactive(expectedCacheKey)).thenReturn(Uni.createFrom().nullItem());
        when(roleRepository.findTrashedRoles(req)).thenReturn(Uni.createFrom().item(emptyResult));
        when(redisService.setWithExpirationReactive(eq(expectedCacheKey), anyString(), eq(300L)))
                .thenReturn(Uni.createFrom().voidItem());

        roleQueryService.findTrashedPaginated(req).await().indefinitely();

        verify(redisService).getReactive(expectedCacheKey);
    }
}
