package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.dto.ShoppingItemRequestDto;
import com.example.Naengbuhae.dto.ShoppingItemResponseDto;
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;

    // 1. 장보기 항목 추가
    @Transactional
    public ShoppingItemResponseDto addShoppingItem(ShoppingItemRequestDto requestDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        ShoppingItem shoppingItem = requestDto.toEntity(user);
        ShoppingItem savedItem = shoppingItemRepository.save(shoppingItem);

        return new ShoppingItemResponseDto(savedItem);
    }

    // 2. 내 장보기 목록 전체 조회
    public List<ShoppingItemResponseDto> getMyShoppingList(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));

        return shoppingItemRepository.findByUser(user).stream()
                .map(ShoppingItemResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 구매 완료 체크 토글 (true <-> false)
    @Transactional
    public ShoppingItemResponseDto toggleCheck(Long id, String username) {
        ShoppingItem shoppingItem = shoppingItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 장보기 항목이 없습니다."));

        // 내 항목이 맞는지 확인! (보안 철저)
        if (!shoppingItem.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 장보기 항목만 상태를 변경할 수 있습니다.");
        }

        // 현재 상태를 반대로 뒤집기 (true면 false로, false면 true로)
        shoppingItem.setChecked(!shoppingItem.isChecked());

        return new ShoppingItemResponseDto(shoppingItem);
    }

    // 4. 장보기 항목 삭제
    @Transactional
    public void deleteShoppingItem(Long id, String username) {
        ShoppingItem shoppingItem = shoppingItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 장보기 항목이 없습니다."));

        // 내 항목이 맞는지 확인!
        if (!shoppingItem.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("본인의 장보기 항목만 삭제할 수 있습니다.");
        }

        shoppingItemRepository.delete(shoppingItem);
    }
}