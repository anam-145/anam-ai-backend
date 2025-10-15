package anam_145.SpringBoot.Server.service.htmlParser;

import anam_145.SpringBoot.Server.apiPayload.code.status.error.CommonErrorStatus;
import anam_145.SpringBoot.Server.apiPayload.exception.handler.HtmlParsingException;
import anam_145.SpringBoot.Server.domain.aiGuide.ComposableInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML 파서 구현체
 * JSoup을 사용하여 HTML UI 요소를 추출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HTMLParserImpl implements HTMLParser {

    @Override
    public List<ComposableInfo> parseHtmlFile(String appId, String fileName, String htmlContent) {
        List<ComposableInfo> composableInfos = new ArrayList<>();

        try {
            // 1. JSoup으로 HTML 파싱
            // baseUri를 빈 문자열로 설정 (상대 경로 해석 불필요)
            Document doc = Jsoup.parse(htmlContent, "");

            // 2. UI 요소별로 추출
            // button 태그 파싱
            composableInfos.addAll(parseButtons(doc, appId, fileName));

            // input 태그 파싱 (텍스트 입력, 체크박스 등)
            composableInfos.addAll(parseInputs(doc, appId, fileName));

            // a 태그 파싱 (링크, 버튼 역할)
            composableInfos.addAll(parseLinks(doc, appId, fileName));

            // form 태그 파싱
            composableInfos.addAll(parseForms(doc, appId, fileName));

            // textarea 태그 파싱
            composableInfos.addAll(parseTextareas(doc, appId, fileName));

            // select 태그 파싱 (드롭다운)
            composableInfos.addAll(parseSelects(doc, appId, fileName));

            // div, span 등 데이터 속성이 있는 요소 파싱
            composableInfos.addAll(parseDataElements(doc, appId, fileName));

            log.debug("HTML 파일 파싱 완료: fileName={}, UI 요소 개수={}", fileName, composableInfos.size());

        } catch (Exception e) {
            log.error("HTML 파싱 실패: fileName={}", fileName, e);
            throw new HtmlParsingException(CommonErrorStatus.HTML_PARSING_FAILED);
        }

        return composableInfos;
    }

    /**
     * <button> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseButtons(Document doc, String appId, String fileName) {
        List<ComposableInfo> buttons = new ArrayList<>();
        Elements buttonElements = doc.select("button");

        for (Element button : buttonElements) {
            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Button") // UI 요소 타입
                    .composableId(extractId(button)) // id 또는 data-* 속성
                    .text(button.text()) // 버튼 라벨 텍스트
                    .semanticHint(extractSemanticHint(button)) // title, aria-label 등
                    .onClickCode(extractOnClick(button)) // onclick 이벤트 핸들러
                    .modifierCode(extractClasses(button)) // class 속성 (스타일 정보)
                    .sourceFile(fileName)
                    .lineNumber(null) // HTML은 라인 번호 추출 어려움 (JSoup 제한)
                    .build();

            buttons.add(info);
        }

        return buttons;
    }

    /**
     * <input> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseInputs(Document doc, String appId, String fileName) {
        List<ComposableInfo> inputs = new ArrayList<>();
        Elements inputElements = doc.select("input");

        for (Element input : inputElements) {
            String inputType = input.attr("type"); // text, password, checkbox 등

            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Input_" + (inputType.isEmpty() ? "text" : inputType)) // Input_text, Input_password 등
                    .composableId(extractId(input))
                    .text(input.attr("placeholder")) // placeholder를 텍스트로 사용
                    .semanticHint(extractSemanticHint(input))
                    .onClickCode(extractOnClick(input))
                    .modifierCode(extractClasses(input))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            inputs.add(info);
        }

        return inputs;
    }

    /**
     * <a> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseLinks(Document doc, String appId, String fileName) {
        List<ComposableInfo> links = new ArrayList<>();
        Elements linkElements = doc.select("a");

        for (Element link : linkElements) {
            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Link")
                    .composableId(extractId(link))
                    .text(link.text()) // 링크 텍스트
                    .semanticHint(link.attr("href")) // href를 semantic hint로 사용
                    .onClickCode(extractOnClick(link))
                    .modifierCode(extractClasses(link))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            links.add(info);
        }

        return links;
    }

    /**
     * <form> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseForms(Document doc, String appId, String fileName) {
        List<ComposableInfo> forms = new ArrayList<>();
        Elements formElements = doc.select("form");

        for (Element form : formElements) {
            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Form")
                    .composableId(extractId(form))
                    .text(null) // form은 텍스트 없음
                    .semanticHint(form.attr("action")) // action 속성
                    .onClickCode(extractOnSubmit(form)) // onsubmit 이벤트
                    .modifierCode(extractClasses(form))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            forms.add(info);
        }

        return forms;
    }

    /**
     * <textarea> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseTextareas(Document doc, String appId, String fileName) {
        List<ComposableInfo> textareas = new ArrayList<>();
        Elements textareaElements = doc.select("textarea");

        for (Element textarea : textareaElements) {
            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Textarea")
                    .composableId(extractId(textarea))
                    .text(textarea.attr("placeholder"))
                    .semanticHint(extractSemanticHint(textarea))
                    .onClickCode(null)
                    .modifierCode(extractClasses(textarea))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            textareas.add(info);
        }

        return textareas;
    }

    /**
     * <select> 태그를 파싱하여 ComposableInfo로 변환
     */
    private List<ComposableInfo> parseSelects(Document doc, String appId, String fileName) {
        List<ComposableInfo> selects = new ArrayList<>();
        Elements selectElements = doc.select("select");

        for (Element select : selectElements) {
            // select의 option들을 텍스트로 결합
            StringBuilder optionsText = new StringBuilder();
            Elements options = select.select("option");
            for (Element option : options) {
                optionsText.append(option.text()).append(", ");
            }

            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type("Select")
                    .composableId(extractId(select))
                    .text(optionsText.toString())
                    .semanticHint(extractSemanticHint(select))
                    .onClickCode(null)
                    .modifierCode(extractClasses(select))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            selects.add(info);
        }

        return selects;
    }

    /**
     * data-* 속성이 있는 div, span 등을 파싱하여 ComposableInfo로 변환
     * 예: <div data-action="transfer" data-target="modal">송금하기</div>
     */
    private List<ComposableInfo> parseDataElements(Document doc, String appId, String fileName) {
        List<ComposableInfo> dataElements = new ArrayList<>();
        // data-로 시작하는 속성이 하나라도 있는 요소 선택
        Elements elements = doc.select("[data-action], [data-target], [data-id], [data-testid]");

        for (Element element : elements) {
            // 이미 다른 메서드에서 처리된 요소는 제외 (button, input 등)
            String tagName = element.tagName().toLowerCase();
            if (tagName.equals("button") || tagName.equals("input") ||
                tagName.equals("a") || tagName.equals("form") ||
                tagName.equals("textarea") || tagName.equals("select")) {
                continue;
            }

            // data-* 속성들을 semantic hint로 결합
            StringBuilder dataAttrs = new StringBuilder();
            element.attributes().forEach(attr -> {
                if (attr.getKey().startsWith("data-")) {
                    dataAttrs.append(attr.getKey()).append("=").append(attr.getValue()).append("; ");
                }
            });

            ComposableInfo info = ComposableInfo.builder()
                    .appId(appId)
                    .type(capitalizeFirst(tagName)) // Div, Span 등
                    .composableId(extractId(element))
                    .text(element.text())
                    .semanticHint(dataAttrs.toString())
                    .onClickCode(extractOnClick(element))
                    .modifierCode(extractClasses(element))
                    .sourceFile(fileName)
                    .lineNumber(null)
                    .build();

            dataElements.add(info);
        }

        return dataElements;
    }

    /**
     * 요소의 ID를 추출
     * 우선순위: id > data-testid > data-id > data-target > class 첫 번째 값 > tagName + text 조합
     *
     * HTML 요소의 고유 식별자를 추출한다.
     * 프론트엔드에서 querySelector로 요소를 찾을 수 있도록 fallback 로직을 제공한다.
     */
    private String extractId(Element element) {
        // 1순위: id 속성
        String id = element.id();
        if (!id.isEmpty()) return id;

        // 2순위: data-testid 속성 (테스트 자동화용 ID)
        String testId = element.attr("data-testid");
        if (!testId.isEmpty()) return testId;

        // 3순위: data-id 속성
        String dataId = element.attr("data-id");
        if (!dataId.isEmpty()) return dataId;

        // 4순위: data-target 속성
        String target = element.attr("data-target");
        if (!target.isEmpty()) return target;

        // 5순위: class 첫 번째 값 (가장 구체적인 클래스명 사용)
        // 예: "send-btn action-btn" → "send-btn"
        String className = element.className();
        if (!className.isEmpty()) {
            String firstClass = className.split("\\s+")[0];
            return firstClass;
        }

        // 6순위: tagName + text 조합 (최후의 수단)
        // 예: <button>Send</button> → "button_Send"
        String tagName = element.tagName();
        String text = element.text();
        if (!text.isEmpty()) {
            // 공백을 언더스코어로 치환하고 최대 20자까지만 사용
            String sanitizedText = text.replaceAll("\\s+", "_");
            int maxLength = Math.min(20, sanitizedText.length());
            return tagName + "_" + sanitizedText.substring(0, maxLength);
        }

        // 모든 fallback 실패 시 null 반환
        return null;
    }

    /**
     * 의미적 힌트 추출
     * 우선순위: title > aria-label > alt
     */
    private String extractSemanticHint(Element element) {
        String title = element.attr("title");
        if (!title.isEmpty()) return title;

        String ariaLabel = element.attr("aria-label");
        if (!ariaLabel.isEmpty()) return ariaLabel;

        String alt = element.attr("alt");
        if (!alt.isEmpty()) return alt;

        return null;
    }

    /**
     * onclick 이벤트 핸들러 추출
     */
    private String extractOnClick(Element element) {
        String onclick = element.attr("onclick");
        return onclick.isEmpty() ? null : onclick;
    }

    /**
     * onsubmit 이벤트 핸들러 추출 (form 전용)
     */
    private String extractOnSubmit(Element element) {
        String onsubmit = element.attr("onsubmit");
        return onsubmit.isEmpty() ? null : onsubmit;
    }

    /**
     * class 속성 추출 (CSS 클래스 리스트)
     */
    private String extractClasses(Element element) {
        String classes = element.className();
        return classes.isEmpty() ? null : classes;
    }

    /**
     * 문자열의 첫 글자를 대문자로 변환
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
