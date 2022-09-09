import React from 'react';
import { FieldError, FieldValues, Path, UseFormRegister } from 'react-hook-form';
import { FormError } from './FormError';

import styles from './Input.module.css';

type InputProps<T extends FieldValues> = {
    name: Path<T>;
    register: UseFormRegister<T>;
    required?: boolean;
    error?: FieldError;
};

export const Input = <T extends FieldValues>({ name, required, register, error }: InputProps<T>) => {
    return (
        <div>
            <input
                id={name}
                className={`${styles.input} ${error ? styles.error : ''}`}
                type="text"
                autoComplete="off"
                {...register(name, { required })}
            />
            <FormError error={error?.message} inputId={name} />
        </div>
    );
};
