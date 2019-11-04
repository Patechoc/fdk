import React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'redux-form';

import InputField from '../../../../components/fields/field-input/field-input.component';
import MultilingualField from '../../../../components/multilingual-field/multilingual-field.component';

const InformationModel = ({ fields, titleLabel, linkLabel, languages }) =>
  fields &&
  fields.map((item, index) => (
    <div className="d-flex flex-column mb-5" key={index}>
      <MultilingualField
        name={`${item}.prefLabel`}
        component={InputField}
        label={titleLabel}
        showLabel
        languages={languages}
      />
      <div className="mt-2">
        <Field
          name={`${item}.uri`}
          component={InputField}
          label={linkLabel}
          showLabel
        />
      </div>
    </div>
  ));

InformationModel.defaultProps = {
  fields: null,
  titleLabel: '',
  linkLabel: '',
  languages: []
};

InformationModel.propTypes = {
  fields: PropTypes.object,
  titleLabel: PropTypes.string,
  linkLabel: PropTypes.string,
  languages: PropTypes.array
};

export default InformationModel;