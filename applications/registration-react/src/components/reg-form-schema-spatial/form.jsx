import React from 'react';
import PropTypes from 'prop-types';
import { Field, FieldArray } from 'redux-form';

import Helptext from '../reg-form-helptext';
import InputTagsFieldArray from '../reg-form-field-input-tags-objects';
import DatepickerField from '../reg-form-field-datepicker';
import CheckboxField from '../reg-form-field-checkbox';
import asyncValidate from '../../utils/asyncValidate';

const renderTemporalFields = (item, index, fields, componentProps) => (
  <div className="d-flex mb-2" key={index}>
    <div className="w-50">
      <Field
        name={`${item}.startDate`}
        type="text"
        component={DatepickerField}
        label="Fra"
        showLabel
      />
    </div>
    <div className="w-50">
      <Field
        name={`${item}.endDate`}
        type="text"
        component={DatepickerField}
        label="Til"
        showLabel
      />
    </div>
    <div className="d-flex align-items-end">
      <button
        className="fdk-btn-no-border"
        type="button"
        title="Remove temporal"
        onClick={
          () => {
            if (fields.length === 1) {
              fields.remove(index);
              fields.push({});
            }

            if (fields.length > 1) {
              fields.remove(index);
            }
            asyncValidate(fields.getAll(), null, componentProps, `remove_temporal_${index}`);
          }
        }
      >
        <i className="fa fa-trash mr-2" />
      </button>
    </div>
  </div>
);

export const renderTemporal = (componentProps) => {
  const { fields } = componentProps;

  return (
    <div>
      {fields && fields.map((item, index) =>
        renderTemporalFields(item, index, fields, componentProps)
      )}
      <button className="fdk-btn-no-border" type="button" onClick={() => fields.push({})}>
        <i className="fa fa-plus mr-2" />
        Legg til tidsperiode
      </button>
    </div>
  );
};

const FormSpatial = props => {
  const { helptextItems, initialValues } = props;
  if (initialValues ) {
    return (
      <form>
        <div className="form-group">
          <Helptext title="Geografisk avgrensning" helptextItems={helptextItems.Dataset_spatial} />
          <Field
            name="spatial"
            type="text"
            component={InputTagsFieldArray}
            label="Geografisk avgrensning"
            fieldLabel="uri"
          />
        </div>
        <div className="form-group">
          <Helptext title="Tidsmessig avgrenset til" helptextItems={helptextItems.Dataset_temporal} />
          <FieldArray
            name="temporal"
            component={renderTemporal}
          />
        </div>
        <div className="form-group">
          <Helptext title="Utgivelsesdato" helptextItems={helptextItems.Dataset_issued} />
          <Field
            name="issued"
            type="text"
            component={DatepickerField}
            label="Utgivelsesdato"
          />
        </div>
        <div className="form-group">
          <Helptext title="Språk" helptextItems={helptextItems.Dataset_language} />
          <Field
            name="language"
            component={CheckboxField}
          />
        </div>

      </form>
    )
  } return null;
}

FormSpatial.defaultProps = {
  initialValues: null
}

FormSpatial.propTypes = {
  initialValues: PropTypes.object,
  helptextItems: PropTypes.object.isRequired,
}

export default FormSpatial
