/*global window, document*/

import { getElementById, sessionStorage } from '../browser/browser';

const STORAGE_KEY = 'gu_id_signIn_state';


class SignInFormModel {
  constructor( formElement, emailField ) {
    this.formElement = formElement;
    this.emailFieldElement = emailField;

    this.addBindings();
    this.smartLock();
  }

  addBindings() {
    this.formElement.on( 'submit', this.formSubmitted.bind( this ) );
  }

  loadState() {
    this.state = SignInFormState.fromStorage();
    this.emailFieldElement.setValue( this.state.email );
  }

  saveState() {
    const email = this.emailFieldElement.value();

    this.state = new SignInFormState( email );
    this.state.save( email );
  }

  smartLockSetupOnSubmit() {

    if (navigator.credentials) {
      const formElement = this.formElement.elem;

      var c = new PasswordCredential(formElement);
      this.smartLockSignIn(c);
    }
  }

  smartLock() {
    if (navigator.credentials) {
      navigator.credentials.get({
          password: true,
        })
        .then(c => {
          if (c instanceof PasswordCredential) {
            c.additionalData = new FormData(document.querySelector('#signin_form'));
            c.idName = "email";
            this.smartLockSignIn(c);
          };
        });
    }
  }

  storeRedirect(c) {
    navigator.credentials.store(c).then(_ => {
      window.location = getElementById('signin_returnUrl').value();
    });
  }

  smartLockSignIn(c) {
    fetch("/actions/signin/smartlock", {credentials: c, method: 'POST'})
      .then(r => {
        if (r.status == 200) {
          this.storeRedirect(c);
          return;
        }
        else {
          r.json().then(j => {
          window.location = j.url;
          return;
        });
      }
    });
  }

  formSubmitted() {
    this.smartLockSetupOnSubmit();
    this.saveState();
  }

  static fromDocument() {
    const form = getElementById( 'signin_form' );
    const emailField = getElementById( 'signin_field_email' );

    if ( form && emailField ) {
      return new SignInFormModel( form, emailField );
    }
  }
}


class SignInFormState {
  constructor( email = "" ) {
    this.email = email;
  }

  save() {
    sessionStorage.setJSON( STORAGE_KEY, this );
  }

  /**
   * @return {SignInFormState}
   */
  static fromObject( { email } = {} ) {
    return new SignInFormState( email );
  }

  static fromStorage() {
    const existingState = sessionStorage.getJSON( STORAGE_KEY );

    return SignInFormState.fromObject( existingState )
  }
}


export function init() {

  const form = SignInFormModel.fromDocument();

  if ( form ) {
    form.loadState();
  }

}
