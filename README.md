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

  



   
