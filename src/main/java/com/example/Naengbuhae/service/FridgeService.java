package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeInvite;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.dto.FridgeResponseDto;
import com.example.Naengbuhae.dto.FridgeResponseDto.MemberDto;
import com.example.Naengbuhae.repository.FridgeInviteRepository;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FridgeService {

    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final FridgeInviteRepository fridgeInviteRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    private static final SecureRandom RANDOM = new SecureRandom();
    // 헷갈리는 문자(0/O, 1/I/l) 제외
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int INVITE_TTL_HOURS = 24;

    // 사용자가 멤버인 모든 냉장고 (소유 + 가입) 반환.
    public List<FridgeResponseDto> listMyFridges(String username) {
        User user = findUser(username);
        return fridgeRepository.findAllForMember(user).stream()
                .map(f -> toDto(f, user))
                .toList();
    }

    public FridgeResponseDto getFridge(Long fridgeId, String username) {
        User user = findUser(username);
        Fridge fridge = findFridge(fridgeId);
        ensureMember(fridge, user);
        return toDto(fridge, user);
    }

    @Transactional
    public FridgeResponseDto createFridge(String name, String username) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("냉장고 이름을 입력해주세요.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("냉장고 이름은 50자 이내여야 합니다.");
        }
        User user = findUser(username);
        Fridge fridge = fridgeRepository.save(new Fridge(user, name.trim()));
        fridgeMemberRepository.save(new FridgeMember(fridge, user));
        return toDto(fridge, user);
    }

    @Transactional
    public FridgeResponseDto renameFridge(Long fridgeId, String name, String username) {
        if (name == null || name.isBlank() || name.length() > 50) {
            throw new IllegalArgumentException("냉장고 이름이 올바르지 않습니다.");
        }
        User user = findUser(username);
        Fridge fridge = findFridge(fridgeId);
        ensureMember(fridge, user);
        fridge.setName(name.trim());
        return toDto(fridge, user);
    }

    // 냉장고 삭제. 멤버 누구나 가능 — 가족 공유에서 위계가 없으므로.
    // 단 모두에게서 사라지는 destructive 작업이므로 클라이언트가 강한 확인을 띄워야 함.
    // 마지막 냉장고는 삭제 불가 (사용자가 식재료 추가할 곳이 없어짐).
    @Transactional
    public void deleteFridge(Long fridgeId, String username) {
        User user = findUser(username);
        Fridge fridge = findFridge(fridgeId);
        ensureMember(fridge, user);

        long myFridgeCount = fridgeRepository.findAllForMember(user).size();
        if (myFridgeCount <= 1) {
            throw new IllegalArgumentException("마지막 냉장고는 삭제할 수 없습니다.");
        }

        // 자식 데이터부터 정리. Hibernate 캐스케이드를 안 걸어둔 상태라 명시적 삭제.
        ingredientRepository.deleteByFridge(fridge);
        fridgeInviteRepository.deleteByFridge(fridge);
        fridgeMemberRepository.deleteByFridge(fridge);
        fridgeRepository.delete(fridge);
    }

    // 초대 코드 발급. 멤버 누구나 가능. 6자리 랜덤 코드, 24시간 유효.
    // 활성 코드가 이미 있으면 그것을 재사용 (새 코드 만들지 않음) — 가족 한 명에게 코드 줬다가
    // "다시 발급" 누르면 옛 코드는 죽고 새 코드만 살아있는 식의 헷갈림 방지.
    @Transactional
    public String createInvite(Long fridgeId, String username) {
        User user = findUser(username);
        Fridge fridge = findFridge(fridgeId);
        ensureMember(fridge, user);

        LocalDateTime now = LocalDateTime.now();
        var existing = fridgeInviteRepository
                .findFirstByFridgeAndExpiresAtAfterOrderByExpiresAtDesc(fridge, now);
        if (existing.isPresent()) {
            return existing.get().getCode();
        }

        // 충돌 방지: 코드 중복이면 재시도 (확률 매우 낮지만 안전망)
        String code;
        int attempts = 0;
        while (true) {
            code = generateCode();
            if (fridgeInviteRepository.findByCode(code).isEmpty()) break;
            if (++attempts > 10) throw new IllegalStateException("초대 코드 생성 실패. 잠시 후 다시 시도해주세요.");
        }
        FridgeInvite invite = new FridgeInvite(fridge, code, now.plusHours(INVITE_TTL_HOURS));
        fridgeInviteRepository.save(invite);
        return code;
    }

    // 초대 코드로 냉장고 가입. 만료 전까지는 여러 명이 같은 코드로 가입 가능 (multi-use).
    // 이미 멤버면 idempotent.
    @Transactional
    public FridgeResponseDto joinByCode(String code, String username) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("초대 코드를 입력해주세요.");
        }
        User user = findUser(username);
        FridgeInvite invite = fridgeInviteRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));
        if (invite.isExpired()) {
            throw new IllegalArgumentException("만료된 초대 코드입니다.");
        }

        Fridge fridge = invite.getFridge();
        boolean alreadyMember = fridgeMemberRepository.existsByFridgeAndUser(fridge, user);
        if (!alreadyMember) {
            fridgeMemberRepository.save(new FridgeMember(fridge, user));

            // 새 멤버 합류 → 기존 멤버들에게 푸시 (가입자 본인 제외)
            List<User> others = fridgeMemberRepository.findByFridge(fridge).stream()
                    .map(FridgeMember::getUser)
                    .filter(u -> !u.getUsername().equals(user.getUsername()))
                    .toList();
            fcmService.sendToUsers(others,
                    "냉장고에 새 멤버가 합류했어요",
                    user.getName() + "님이 '" + fridge.getName() + "'에 참여했습니다.");
        }
        return toDto(fridge, user);
    }

    // 멤버 제거. 멤버 누구나 가능. 자기 자신은 leave 엔드포인트 사용.
    @Transactional
    public void removeMember(Long fridgeId, String targetUsername, String requesterUsername) {
        User requester = findUser(requesterUsername);
        Fridge fridge = findFridge(fridgeId);
        ensureMember(fridge, requester);

        if (targetUsername.equals(requesterUsername)) {
            throw new IllegalArgumentException("본인은 '나가기' 기능을 사용해주세요.");
        }
        User target = findUser(targetUsername);
        if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, target)) {
            throw new IllegalArgumentException("멤버가 아닙니다.");
        }
        fridgeMemberRepository.deleteByFridgeAndUser(fridge, target);

        // 제거된 본인에게 알림
        fcmService.sendToUser(target.getUsername(),
                "냉장고에서 제외되었어요",
                "'" + fridge.getName() + "'에서 더 이상 멤버가 아닙니다.");
    }

    // 본인이 가입한 냉장고에서 나가기. 마지막 멤버였다면 냉장고 자체를 정리.
    @Transactional
    public void leaveFridge(Long fridgeId, String username) {
        User user = findUser(username);
        Fridge fridge = findFridge(fridgeId);
        if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
            throw new IllegalArgumentException("멤버가 아닙니다.");
        }
        long myFridgeCount = fridgeRepository.findAllForMember(user).size();
        if (myFridgeCount <= 1) {
            throw new IllegalArgumentException("마지막 냉장고는 나갈 수 없습니다.");
        }
        fridgeMemberRepository.deleteByFridgeAndUser(fridge, user);

        // 모든 멤버가 나갔으면 냉장고 자체 정리 (식재료/초대코드 포함)
        if (fridgeMemberRepository.findByFridge(fridge).isEmpty()) {
            ingredientRepository.deleteByFridge(fridge);
            fridgeInviteRepository.deleteByFridge(fridge);
            fridgeRepository.delete(fridge);
        }
    }

    // ===== 헬퍼 =====

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Fridge findFridge(Long fridgeId) {
        return fridgeRepository.findById(fridgeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 냉장고가 없습니다."));
    }

    private void ensureMember(Fridge fridge, User user) {
        if (!fridgeMemberRepository.existsByFridgeAndUser(fridge, user)) {
            throw new IllegalArgumentException("이 냉장고에 접근 권한이 없습니다.");
        }
    }

    // 멤버는 모두 동등 — isOwner는 더 이상 권한이 아니라 "내가 만든 냉장고인지"의 단순 정보로만 유지.
    private FridgeResponseDto toDto(Fridge fridge, User viewer) {
        List<MemberDto> members = fridgeMemberRepository.findByFridge(fridge).stream()
                .map(fm -> {
                    User u = fm.getUser();
                    boolean isCreator = fridge.getOwner().getUsername().equals(u.getUsername());
                    return new MemberDto(u.getUsername(), u.getName(), isCreator);
                })
                .collect(Collectors.toList());
        boolean viewerIsCreator = fridge.getOwner().getUsername().equals(viewer.getUsername());
        return new FridgeResponseDto(fridge, members, viewerIsCreator);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
