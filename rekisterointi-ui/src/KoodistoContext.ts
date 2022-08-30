import React, { useContext } from 'react';
import { Koodi } from './types';

export interface Koodistos {
    kunnat: Koodi[];
    yritysmuodot: Koodi[];
    organisaatiotyypit: Koodi[];
    maat: Koodi[];
}

const defaultKoodistos = {
    kunnat: [],
    yritysmuodot: [],
    organisaatiotyypit: [],
    maat: [],
};

export const KoodistoContext = React.createContext<Koodistos>(defaultKoodistos);

export const useKoodistos = () => {
    const context = useContext(KoodistoContext);
    if (!context && context !== null) {
        throw new Error('Koodisto not available');
    }
    return context;
};