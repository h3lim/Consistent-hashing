# Consistent Hashing 프로젝트

Consistent Hashing을 활용한 Shard 기반 분산 데이터 시스템. 각 사용자 데이터를 다양한 Shard에 분산하여 저장하고 관리합니다. Shard의 추가 및 제거 시, 데이터 재분배 및 자동 마이그레이션 기능을 통해 시스템의 일관성과 안정성을 유지합니다.

---

## 목차
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
  - [데이터 분산 및 마이그레이션](#데이터-분산-및-마이그레이션)
  - [WebSocket을 통한 실시간 대시보드](#websocket을-통한-실시간-대시보드)
  - [사용자 관리 기능](#사용자-관리-기능)
- [사용 기술](#사용-기술)
  - [백엔드](#백엔드)
  - [프론트엔드](#프론트엔드)
  - [데이터베이스](#데이터베이스)
  - [빌드 도구](#빌드-도구)
- [참고 사항](#참고-사항)
  - [CORS 설정](#cors-설정)
  - [OAuth 설정](#oauth-설정)
  - [SSE 연결](#sse-연결)
  - [로컬 개발 환경](#로컬-개발-환경)

---

## 프로젝트 구조

```bash
consistent_hashing_project
└── src
    └── main
        └── java
            └── com
                └── example
                    └── consistent_hashing
                        ├── config                  # 설정 관련 클래스 모음
                        │   ├── DataSourceConfig.java        # 데이터 소스 설정, 여러 Shard 데이터 소스 구성
                        │   ├── IdGeneratorConfig.java       # 사용자 ID 생성기 설정
                        │   ├── ShardDataSourceProperties.java # Shard 데이터 소스 속성 관리
                        │   ├── ShardRoutingDataSource.java   # Shard 라우팅 기능 제공하는 데이터 소스
                        │   └── WebSocketConfig.java         # WebSocket 설정
                        ├── controller              # REST API 및 WebSocket 컨트롤러
                        │   ├── DashboardController.java      # 대시보드 관련 데이터 조회 컨트롤러
                        │   ├── DashboardViewController.java  # 대시보드 페이지 접근 컨트롤러
                        │   ├── ShardController.java          # Shard 추가/삭제 및 분산 조회 기능 컨트롤러
                        │   ├── UserController.java           # 사용자 생성/조회 기능 컨트롤러
                        │   └── WebSocketController.java      # WebSocket으로 Shard 업데이트 알림 전송
                        ├── dto                     # 데이터 전송 객체 (DTO) 모음
                        │   ├── MigrationStatistics.java      # Shard 간 데이터 이동 통계 DTO
                        │   └── ShardDetail.java             # 각 Shard의 사용자 수 정보 DTO
                        ├── entity                  # JPA 엔티티 클래스
                        │   └── User.java                    # 사용자 엔티티
                        ├── exception               # 커스텀 예외 클래스
                        │   ├── ShardException.java          # Shard 관련 예외 처리 클래스
                        │   └── UserMigrationException.java  # 사용자 이동 중 예외 처리 클래스
                        ├── hashing                 # Consistent Hashing 구현 클래스
                        │   └── ConsistentHashing.java       # Consistent Hashing 기능 제공 클래스
                        ├── repository             # JPA 리포지토리 인터페이스
                        │   └── UserRepository.java          # 사용자 리포지토리 인터페이스
                        ├── service                # 비즈니스 로직 서비스 클래스 모음
                        │   ├── ConsistentHashingService.java # Consistent Hashing 기능을 사용하는 서비스
                        │   ├── ShardService.java            # Shard 관리 및 데이터 이동 서비스
                        │   └── UserService.java             # 사용자 저장, 조회 및 업데이트 서비스
                        └── util                   # 유틸리티 클래스
                            └── CustomIdGenerator.java       # 커스텀 ID 생성기
```
---

## 주요 기능

### 1. 데이터 분산 및 마이그레이션
- **Consistent Hashing 기반 데이터 분산**: 사용자 데이터를 각 Shard에 균등하게 분산하여 저장합니다.
- **Shard 추가/제거 시 자동 데이터 마이그레이션**: Shard가 추가되거나 제거될 때 데이터를 재분배하여 일관성을 유지합니다.

### 2. WebSocket을 통한 실시간 대시보드
- **대시보드 실시간 업데이트**: 사용자 수, Shard 분포도 등의 통계를 WebSocket을 통해 실시간으로 업데이트합니다.
- **차트 및 그래프**: 각 Shard에 저장된 데이터 및 마이그레이션 현황을 시각적으로 제공합니다.

### 3. 사용자 관리 기능
- **사용자 Shard 할당**: Consistent Hashing을 기반으로 사용자 데이터를 특정 Shard에 저장합니다.

---

## 사용 기술

### 백엔드

- **Spring Boot**
- **Spring Data JPA**
- **WebSocket**
- **Consistent Hashing**
- **Custom ID Generator**

### 프론트엔드

- **HTML5, CSS3, JavaScript**
- **Bootstrap 5**
- **Chart.js**
- **SockJS & STOMP.js**
- **SVG 그래픽 요소**

### 데이터베이스

- **Oracle Database**
### 빌드 도구

- **Gradle**

---

## 참고 사항

- **Shard 구성**: ShardDataSourceProperties 클래스에서 Shard 정보 설정 필요
- **WebSocket 사용 시**: 로컬 개발 환경에서 WebSocket 설정 (localhost) 필요
- **CORS 설정**: WebSocket 연결 시 CORS 설정 필요
- **로컬 개발 환경**: OAuth 및 WebSocket 기능 테스트를 위한 개발용 API 키 설정 필요
