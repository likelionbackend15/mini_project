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

java \
—module-path "/Library/Java/Myli/javafx-sdk-21.0.6/lib" \
—add-modules javafx.controls,javafx.fxml,javafx.media \
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
   
