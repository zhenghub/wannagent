package org.freefeeling.bootstrap

import com.karasiq.bootstrap.Bootstrap
import com.karasiq.bootstrap.BootstrapImplicits._
import com.karasiq.bootstrap.form.{Form, FormInput, FormInputGroup}
import com.karasiq.bootstrap.icons.FontAwesome
import com.karasiq.bootstrap.modal.Modal
import org.scalajs.dom.window
import rx._

import scalatags.JsDom.all._

object TestModal {
  def apply()(implicit ctx: Ctx.Owner): Modal = {
    val modalInputValue = Var("10000000") // Better use string
    val option2Title = Var("Option 2")
    val radioGroup = FormInput.radioGroup(FormInput.radio("Test1", "modal-title", "First radio"), FormInput.radio("Test2", "modal-title", "Second radio"))
    val select = FormInput.select("Plain select", "Option 1" → "Option 1", "Option 2" → option2Title, "Option 3" → "Option 3")
    val multipleSelect = FormInput.simpleMultipleSelect("Multiple select", "Option 1", "Option 2", "Option 3")
    val form = Form(
      FormInputGroup(FormInputGroup.label("Money"), FormInputGroup.addon("usd".fontAwesome(FontAwesome.fixedWidth)), FormInputGroup.number(modalInputValue.reactiveInput)),
      radioGroup,
      select,
      multipleSelect,
      FormInput.textArea("Money text area", rows := 1, modalInputValue.reactiveInput),
      FormInput.file("Test file", onchange := Bootstrap.jsInput { input ⇒
        val file = input.files.head
        window.alert(s"File selected: ${file.name}")
      })
    )

    select.selected() = Seq("Option 2")
    option2Title() = "Changed option 2"

    Modal()
      .withTitle(radioGroup.value, " / ", select.selected.map(_.head), " / ", multipleSelect.selected.map(_.mkString(" + ")))
      .withBody(p("You won ", modalInputValue, "$"), p(form))
      .withButtons(Modal.closeButton(), Modal.button("Take", Modal.dismiss))
  }
}