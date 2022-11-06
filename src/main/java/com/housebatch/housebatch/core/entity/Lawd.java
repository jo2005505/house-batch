package com.housebatch.housebatch.core.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

// 동 정보 테이블 Entity
@Entity
@Getter
@Setter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Table(name = "lawd")
public class Lawd {

    /**
     * ** 실행환경 **
     * OS       : Ubuntu
     * Database : MySQL
     * - 문제 사항 : Linux 상에서 MySQL의 테이블명은 대소문자를 구분하며, JPA에서는 테이블 명을 소문자로 치환하여 쿼리문이 작성된다.
     * ** 실행환경 **
     *
     * JPA에서는 테이블과 Entity간의 네이밍 맵핑 규칙이 설정된다.
     * - spring.jpa.hibernate.naming.implicit-strategy : 논리명 생성, 명시적으로 컬럼과 테이블 명이 존재하지 않는 경우 논리명 적용
     * - spring.jpa.hibernate.naming.physical-strategy : 물리명 적용, 모든 논리명이 적용되고 실제 테이블에 적용
     *
     * 기본 설정
     * - spring.jpa.hibernate.naming.implicit-strategy: org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy
     * - spring.jpa.hibernate.naming.physical-strategy: org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy
     *      1) 카멜 케이스 -> 언더스코어  (memberPoint -> member_point)
     *      2) .(점) -> _(언더스코어)
     *      3) 대문자 -> 소문자
     *
     * 별도 설정 - 입력한 테이블과 컬럼명으로 데이터베이스 정보 조회
     * - spring.jpa.hibernate.naming.physical-strategy: physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
     *
     * 참고 : https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-access.configure-hibernate-naming-strategy
     *
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lawdId;

    @Column(nullable = false)
    private String lawdCd;

    @Column(nullable = false)
    private String lawdDong;

    @Column(nullable = false)
    private Boolean exist;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
