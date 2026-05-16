package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.FridgeResponseDto;
import com.example.Naengbuhae.service.ActivityLogService;
import com.example.Naengbuhae.service.FridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fridges")
@RequiredArgsConstructor
public class FridgeController {

    private final FridgeService fridgeService;
    private final ActivityLogService activityLogService;

    // 내가 멤버인 모든 냉장고
    @GetMapping
    public List<FridgeResponseDto> list(Principal principal) {
        return fridgeService.listMyFridges(principal.getName());
    }

    // 특정 냉장고 상세 (멤버 목록 포함)
    @GetMapping("/{id}")
    public FridgeResponseDto get(@PathVariable UUID id, Principal principal) {
        return fridgeService.getFridge(id, principal.getName());
    }

    // 새 냉장고 생성
    @PostMapping
    public FridgeResponseDto create(@RequestBody Map<String, String> body, Principal principal) {
        return fridgeService.createFridge(body.get("name"), principal.getName());
    }

    // 이름 변경 (owner 전용)
    @PutMapping("/{id}")
    public FridgeResponseDto rename(@PathVariable UUID id,
                                    @RequestBody Map<String, String> body,
                                    Principal principal) {
        return fridgeService.renameFridge(id, body.get("name"), principal.getName());
    }

    // 냉장고 삭제 (owner 전용, 마지막 냉장고는 삭제 불가)
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id, Principal principal) {
        fridgeService.deleteFridge(id, principal.getName());
        return Map.of("success", true);
    }

    // 초대 코드 발급 (owner 전용). 응답: { code: "AB12CD" }
    @PostMapping("/{id}/invites")
    public Map<String, String> createInvite(@PathVariable UUID id, Principal principal) {
        return Map.of("code", fridgeService.createInvite(id, principal.getName()));
    }

    // 초대 코드로 가입
    @PostMapping("/join")
    public FridgeResponseDto join(@RequestBody Map<String, String> body, Principal principal) {
        return fridgeService.joinByCode(body.get("code"), principal.getName());
    }

    // 멤버 제거 (owner 전용)
    @DeleteMapping("/{id}/members/{username}")
    public Map<String, Object> removeMember(@PathVariable UUID id,
                                            @PathVariable String username,
                                            Principal principal) {
        fridgeService.removeMember(id, username, principal.getName());
        return Map.of("success", true);
    }

    // 나가기 (멤버 본인이 owner 아닌 냉장고에서)
    @PostMapping("/{id}/leave")
    public Map<String, Object> leave(@PathVariable UUID id, Principal principal) {
        fridgeService.leaveFridge(id, principal.getName());
        return Map.of("success", true);
    }

    // 가족 활동 통계 — 기간(days) 기본 30, 최대 365.
    @GetMapping("/{id}/activity-stats")
    public ActivityLogService.Stats activityStats(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "30") int days,
                                                  Principal principal) {
        return activityLogService.getStats(id, days, principal.getName());
    }
}
