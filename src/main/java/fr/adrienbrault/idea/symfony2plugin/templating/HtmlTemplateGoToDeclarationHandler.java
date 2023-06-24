package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class HtmlTemplateGoToDeclarationHandler implements GotoDeclarationHandler {
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        Collection<PsiElement> targets = new ArrayList<>();

        // <twig:a
        if (psiElement instanceof XmlToken && psiElement.getNode().getElementType() == XmlTokenType.XML_NAME && psiElement.getText().startsWith("twig:")) {
            String text = psiElement.getText();
            if (!text.startsWith("twig:")) {
                return null;
            }


            int calulatedOffset = offset - psiElement.getTextRange().getStartOffset();
            if (calulatedOffset < 0) {
                calulatedOffset = 5;
            }

            // <twig:a
            if (calulatedOffset > 5) {
                if (TwigHtmlCompletionUtil.getTwigNamespacePattern().accepts(psiElement)) {

                    String componentName = StringUtils.stripStart(text, "twig:");
                    if (!componentName.isBlank()) {
                        targets.addAll(UxUtil.getTwigComponentNameTargets(psiElement.getProject(), componentName));
                    }
                }
            } else {
                // <twig
                targets.addAll(UxUtil.getTwigComponentAllTargets(psiElement.getProject()));
            }
        }

        // <twig:Foo :message="" message="">
        if (psiElement instanceof XmlToken) {
            PsiElement parent = psiElement.getParent();
            if (parent.getNode().getElementType() == XmlElementType.XML_ATTRIBUTE) {
                if (parent.getParent() instanceof HtmlTag htmlTag && htmlTag.getName().startsWith("twig:")) {
                    String text = psiElement.getText();

                    for (PhpClass phpClass : UxUtil.getTwigComponentNameTargets(psiElement.getProject(), htmlTag.getName().substring(5))) {
                        Field fieldByName = phpClass.findFieldByName(StringUtils.stripStart(text, ":"), false);
                        if (fieldByName != null) {
                            targets.add(fieldByName);
                        }
                    }
                };
            }
        }

        return targets.toArray(new PsiElement[0]);
    }
}