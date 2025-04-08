# wearther-bot

## 🔖 깃 가이드라인
#### <u>본인 파일 외 다른 사람 파일 수정/삭제 금지</u>
1. 원본 레포지토리 `fork`
    ![img.png](img.png)

2. 포크해 간 <u>본인 레포지토리로 이동 후</u> `clone`
    ```
    git clone { 본인 repo 주소 }
    ```
3. 로컬 환경에서 <u>본인 이름 이니셜</u>로 브랜치 생성 후 문서 작성
    ```
    git checkout -b { 본인 이름 이니셜 } // 로컬 브랜치 생성 후 생성한 브랜치로 이동
   
   ex. git checkout -b dy
   ```
4. 문서 작성 후 `push`
    - **커밋 메세지 형식**: `Add: {책번호}/{주차}/{챕터}/{이니셜}`
    - 문서를 수정할 경우 커밋 메시지 형식: `Edit: {책번호}/{주차}/{챕터}/{이니셜}`
    - <u>커밋은 한 챕터 문서 작성 완료마다 할 수 있도록 함: 1챕터당 1커밋</u> 
    ```
   git add . // 작업한 모든 내용(.) 스테이징
    git commit -m '{ commit_message }' // 커밋 메세지 작성 & 커밋
    git push origin { 지금까지 작업한 브랜치 이름 } // 원격 저장소의 브랜치에 push
   
   ex.
    git add .
    git commit -m 'Add: 01/week01/ch01/dy' 
    git push origin dy
    ```

5. 웹 페이지에서 `PR(Pull Request)` 작성
    - 원본 레포지토리의 `main` 브랜치로 `PR`
    - `PR` 제목 형식: `{책번호}/{주차}/{챕터}/{이니셜}`
      - `ex. 01/week01/ch01-02/dy` : 커밋 메세지와 챕터 부분만 다르게 작성


6. `PR Merge` 이후 본인 레포지토리로 `pull`
```
git pull upstream main // 원본 레포지토리 main 브랜치를 본인 로컬로 pull
git push origin { 본인 브랜치 이름 } // 본인의 원격 레포 브랜치로 새로 받아온 내용 push
```
