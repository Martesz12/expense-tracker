// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const ngrx = require('@ngrx/eslint-plugin/v9');
const rxjsXRaw = require('eslint-plugin-rxjs-x');
const rxjsX = rxjsXRaw.default || rxjsXRaw;
const prettierConfig = require('eslint-config-prettier');

module.exports = defineConfig([
  {
    ignores: ['dist/**', '.angular/**', 'node_modules/**'],
  },
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
      ...ngrx.configs.store,
      ...ngrx.configs.effects,
      ...ngrx.configs.operators,
      rxjsX.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: __dirname,
      },
    },
    processor: angular.processInlineTemplates,
    rules: {
      // Angular selectors
      '@angular-eslint/directive-selector': [
        'error',
        { type: 'attribute', prefix: 'app', style: 'camelCase' },
      ],
      '@angular-eslint/component-selector': [
        'error',
        { type: 'element', prefix: 'app', style: 'kebab-case' },
      ],
      '@angular-eslint/prefer-on-push-component-change-detection': 'warn',

      // TypeScript — loosen a few defaults for "not too strict"
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/explicit-function-return-type': 'off',

      // RxJS — downgrade preference rules to warn
      'rxjs-x/no-nested-subscribe': 'warn',
      'rxjs-x/prefer-observer': 'warn',
      'rxjs-x/prefer-root-operators': 'warn',
      'rxjs-x/no-topromise': 'warn',
      'rxjs-x/no-sharereplay': 'warn',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
  {
    // Relax rules in test files
    files: ['**/*.spec.ts'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
    },
  },
  prettierConfig,
]);
