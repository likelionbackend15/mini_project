# mini_project
미니 프로젝트 1차 (회고 1팀)

**<환경조건>**

자바 21 이상
javafx sdk 21.0.6 이상

**<프로그램 실행 방법>**

터미널 창에 jar 파일이 위치한 곳에 터미널을 열고
  <img width="776" alt="스크린샷 2025-05-09 오후 4 51 28" src="https://github.com/user-attachments/assets/ad7e78a9-9d46-4ce8-bc41-b7c4ae3bff30" />


다음과 같은 명령어를 입력해서 jar 파일을 실행(단, java sdk 모듈 경로는 본인 컴퓨터 환경에 맞게 지정)

   <img width="565" alt="image" src="https://github.com/user-attachments/assets/6b25ff99-991a-4d7f-96bc-37a8b36a4b7c" />

**예시)**

java --module-path "/Library/Java/Mylib/javafx-sdk-21.0.6/lib" \
     --add-modules javafx.controls,javafx.fxml,javafx.media \
     -jar client-all.jar


**<핵심 기능 및 사용 설명서>**

**로그인 기능**

<img width="411" alt="image" src="https://github.com/user-attachments/assets/27d7266e-f255-450b-a8bb-3f2b2047703d" />

- 기존 계정으로 바로 서비스에 입장
- 아이디·비밀번호를 입력하면 서버가 DB의 Bcrypt 해시와 대조해 즉시 인증을 끝냄

**회원가입 기능**

<img width="432" alt="image" src="https://github.com/user-attachments/assets/3d511860-de81-40f1-8e43-c93572e62446" />

- 새 사용자는 아이디·닉네임·이메일을 입력하고 중복 검사를 통과해야 함
- 아이디, 이메일은 중복 안됨
- 닉네임은 중복 허용

<img width="357" alt="image" src="https://github.com/user-attachments/assets/f453fa15-1dd4-4eae-b34e-6d8b89c8e0ca" />

- 이메일을 입력하고 send code 버튼을 누르면 이메일로 인증 번호 발송됨
  
<img width="650" alt="image" src="https://github.com/user-attachments/assets/f9b49ac4-2d8a-466d-8794-d19875764f9b" />

- 입력한 이메일로 인증 코드 확인
- 이 인증코드를 회원가입 창에 입력

**로비화면 기능**


![스크린샷 2025-05-09 17 29 40](https://github.com/user-attachments/assets/76d89bcb-47ca-4ac3-9db8-e51992534d59)

- RoomList, Create Study Room, MyInfo 중 원하는 버튼 클릭 시 해당 기능 화면으로 즉시 전환
- 네비게이션만 담당해 로비에서는 별도의 서버 요청 없이 Scene 전환만 수행

**셋팅화면 기능**


![스크린샷 2025-05-09 17 30 37](https://github.com/user-attachments/assets/ee803390-cd12-4175-aa5b-ccbff9ece9d3)

- 상단에 자신의 ID, Email이 표시
- change Password, LogOut, Delete Account, Back 중 원하는 버튼 클릭 시 해당 기능 화면으로 즉시 전환
- LogOut 버튼 클릭 시 세션, 토큰을 모두 삭제하고 로그인 화면으로 전환
- Back 버튼 클릭 시 로비화면으로 전환

**비밀번호 변경화면 기능**


![스크린샷 2025-05-09 17 32 24](https://github.com/user-attachments/assets/329e4a6a-d06d-48f4-bab9-48079922494a)

- 이메일 입력 시 해당 이메일로 인증코드 발송
- 사용자가 인증 코드 입력 후 새 비밀번호 두 번 입력
- Change Password 클릭 시 비밀번호 변경 완료
- Back to the Lobby 클릭 시 로비화면으로 전환


**회원탈퇴화면 기능**


![스크린샷 2025-05-09 17 35 55](https://github.com/user-attachments/assets/03c4a191-7e34-4483-ae81-c3c32f842c62)

- 셋팅화면에서 Delete Account 버튼 클릭 시 위 확인창 알림

![스크린샷 2025-05-09 17 37 21](https://github.com/user-attachments/assets/09df7ade-984b-430a-b4bc-692a0e312881)

- 확인 클릭 시 Delete Account 화면으로 전환
- 자신의 패스워드 입력 후 Delete Account 클릭 시 계정 삭제
- Cancel 클릭 시 로비화면으로 전환


**로그아웃 기능**


![스크린샷 2025-05-09 17 39 16](https://github.com/user-attachments/assets/8537d16f-6c3d-4a79-a38b-a036fd4a75de)

- 셋팅화면에서 LogOut 클릭 시 위 알림창이 뜸
- 확인 클릭 시 로그아웃 상태로 로그인화면으로 전환
- 취소 클릭 시 현재 화면으로 전환
   
**방 만들기**

![](https://velog.velcdn.com/images/hyojin0911/post/edbca7f8-2947-4702-914e-cb5fb22aa2f3/image.png)
 - 두 번째 Create Study Room 선택 화면
   - 기본적인 방의 이름(Room Name) 및 기타옵션을 설정
     - 집중시간(Focus Time) / 쉬는시간(Break Time) / 회차(Loops)
   - 방의 최대 수용 인원을 설정.
     - Max Members
   - 스터디 진행중 중간입장 가능 여부(Mid Entry) / 비밀방 설정(Private Room)
   - Create 클릭시 다음 대기 화면으로 Scene 전환
   - Back 클릭시 이전 로비 화면으로 Scene 전환

**대기 화면**

![](https://velog.velcdn.com/images/hyojin0911/post/581df7d8-cbe2-400f-bfa5-97381f08cd55/image.png)
 - 스터디방 생성 이후 대기방
   - 방장이 Host가 되어 또 다른 사용자를 기다리는 대기방
   - 상단 중앙에 방 이름, Private의 여부, 가능한 최대 인원 확인 가능
   - 방 생성시 설정한 인원대로 입장 가능
 - 하단에 Start Focus 클릭하면 채팅방으로 이동하며 이와 동시에 Pomodoro Timer가 작동하여 시간 체크가 가능


**룸 리스트 화면**




- 로비에서 Room List 버튼 선택 시 전환되는 화면
  -![image](https://github.com/user-attachments/assets/41e11954-bb83-4d17-89e8-51870ee516bf)





- 새로고침 선택 시 방 리스트 출력
  - ![image](https://github.com/user-attachments/assets/fbf9db6a-17bd-4c47-9a9a-9836523eec85)
  - 일반 중간입장 허용 방은 방 선택 후 입장하기 버튼 입력 시 입장.
  - Private Room은 방 이름 앞에 🔒 표시로 구분 가능
  - 현재 인원 / 최대 인원 / 사이클 수 / 중간입장 가능 여부 표시 / 방장의 ID 한 줄로 표시
  - 중간 입장 false인 방 입장 시도시 '이 방은 중간 입장이 허용되지 않습니다' 메세지 좌측 하단에 표시
  - ![image](https://github.com/user-attachments/assets/98650347-d8ad-469d-b19d-f9e84889f54d)



![image](https://github.com/user-attachments/assets/39781d2e-90e4-44db-92dd-3da3c6cc8b8a)

- Private Room 선택 후 입장하기 버튼 입력 시, 비밀번호 입력 팝업 발생.
  - 설정된 비밀번호가 아닐 시 오류 메세지 출력.
  - ![image](https://github.com/user-attachments/assets/4725652d-62e9-4e55-b071-70c4e4be470a)
  - 설정된 비밀번호가 맞으면 입장.



**Pomodoro화면**




![image](https://github.com/user-attachments/assets/6fb3f515-b198-446f-85fa-27b32cec9391)

- 방장이 Start Focus 버튼을 누루면 서버가 모든 클라이언트에게 타이머를 일괄전송해 카운트 다운을 시작한다
  - 화면 중앙 상단에 원형 타이머(focus 상태) 중앙에 타이머 속 중앙에 남은 시간이 MM:SS 형식으로 1초 단위로 갱신된다
  - 우측 패널에는 참가자 목록이 표시된다
 

![image](https://github.com/user-attachments/assets/e1b51e4f-5599-4e24-b58c-bc6f85992140)

 - 하단 채팅창에는 메시지 혹은 이모지를 입력할 수 있고 enter 또는 오른쪽 하단의 send 버튼을 눌러 메시지를 전송할 수 있다
 - 전송된 메시지는 채팅 로그에 추가되고 서버가 모든 참가자에게 chat 패킷을 브로드 캐스트 한다
 - 남은 시간이 0이되면 집중 상태가 종료된다
 
![image](https://github.com/user-attachments/assets/1e8a2323-8863-41e7-b6dd-87d49a80baa8)

 - 집중 종료시 중앙 텍스트가 "Break"로 변경된다
 - 채팅 입력 및 전송 로직은 집중 단계와 동일하다
 - 휴식 단계에 남은 시간이 0이 되면 다음 집중 사이클로 자동 전환된다
 - 모든 사이클을 완료시 원형 타이머가 00:00 상태가 된다 
     

